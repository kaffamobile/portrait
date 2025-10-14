# Portrait Agent Briefing

## High-Level Context
- Portrait is an ahead-of-time reflection library for Java/Kotlin with a unified API that runs on JVM, TeaVM (JS/WASM), GraalVM Native Image, and other restricted environments.
- Core promise: “Write once, reflect anywhere” by swapping between runtime reflection (JVM) and generated code (`*$Portrait` classes) without changing application code.
- Modules: `portrait-annotations` → `portrait-api` → (`portrait-runtime-jvm`, `portrait-runtime-aot`, `portrait-codegen`, `tests-e2e`). Dependencies are managed via `gradle/libs.versions.toml`; never inline versions.

## Key Architectural Concepts
- Providers discovered via `ServiceLoader`, ordered by priority: WellKnown (200) → Generated (150) → JVM (100) → custom (0–299). Always return `null` instead of throwing to let the next provider try.
- `PClass`, `PMethod`, `PField`, and `PConstructor` abstractions wrap class metadata, invocation, and access. Generated code uses integer IDs and table switches for O(1) dispatch.
- Caching uses `ConcurrentHashMap` with a sentinel to prevent circular dependency blowups; property access is lazy-loaded (superclass, interfaces, etc.).
- Codegen phases: classpath scan (ClassGraph+ASM) → annotation collection (`@Reflective`, `@ProxyTarget`, opt-ins) → ByteBuddy generation of `*$Portrait` classes plus binary metadata with string pooling.
- Codegen module must not load target classes via Portrait runtime; it only references types symbolically.

## Testing Guidance
- Local classes are unsupported. Define fixtures at top level or in dedicated `TestFixtures.kt`.
- Expect `NullPointerException`, `AssertionFailedError`, or `RuntimeException` when providers are missing; treat `StackOverflowError` or primitive `ClassNotFoundException` as red flags.
- Module coverage expectations: annotations, api, runtime-jvm, runtime-aot, codegen; `tests-e2e` verifies end-to-end reflection across generated and runtime paths.
- `./gradlew :tests-e2e:test` runs JVM E2E tests. TeaVM tests require Java 11+, enabling the TeaVM plugin, configuring the runner, then executing `./gradlew :tests-e2e:testJs`.

## Current TODO Signals
- Guarantee TeaVM support by testing `portrait-runtime-aot` under TeaVM.
- Dogfooding: drop `.wellknown` in `portrait-api` and switch to running `portrait-codegen` with `@OptInPortrait`.
- Post v1.0 ideas: JVM flag for AoT emulation, reflections/ClassGraph-like discovery, Maven and Gradle plugins for codegen.

## Practical Reminders for Agents
- Follow version-catalog conventions for dependencies; add entries in `[versions]` and reference via `libs.*`.
- When debugging resolution, confirm providers are registered in `META-INF/services/tech.kaffa.portrait.provider.PortraitProvider`.
- Portrait handles Java primitives directly; failures there indicate provider or lookup regressions.
- For proxies, use `ProxyHandler` callbacks; generated proxies dispatch via method IDs.
- Keep metadata compatibility in mind: binary blobs start with magic/version headers and rely on string pooling and flag bitmasks.

## Useful Entry Points
- `Portrait.of(...)`, `Portrait.from(...)`, `Portrait.forName(...)`, `Portrait.forNameOrUnresolved(...)`.
- Extension helpers: `MyClass::class.portrait`, `MyClass::class.java.portrait`, `instance.portrait`.
- Typical fixtures covered in `tests-e2e`: sealed hierarchies (`Operation`), proxies (`Calculator`), enums (`Status`), generics (`Container<T>`), nullable types, multi-ctor classes, objects (`SingletonService`).

## Troubleshooting Cheat Sheet
- Missing reflection: ensure class is annotated or included via opt-in annotations.
- `RuntimeException: No PortraitProvider`: check service registration and provider availability on classpath.
- Provider recursion or deadlock: inspect sentinel usage and lazy blocks inside `JvmP*`/`StaticP*` implementations.
- If TeaVM/Native step fails, verify generated metadata alignment and that restricted environments have the correct provider order.
