# Portrait

Portrait lets you "write once, reflect anywhere." It provides a single reflection API that works on the desktop JVM and
in restricted environments such as GraalVM Native Image and TeaVM, where `java.lang.reflect` is unavailable or heavily
sandboxed.

- Deterministic code generation for reflection metadata
- Service loader driven provider discovery
- Focused surface area: public classes, constructors, fields, and methods
- First-class support for AOP and interface proxying

## Why Portrait

Traditional reflection fails or is disallowed in ahead-of-time and sandboxed runtimes. Portrait replaces ad‑hoc
reflection with generated `*$Portrait` companions that are discovered through `ServiceLoader` at runtime. Application
code keeps calling the Portrait API while the library swaps between the JVM runtime provider and generated providers
depending on the platform.

Portrait is opt-in by design: only the classes you mark are included, keeping metadata minimal and build times
predictable.

## Module Overview

| Module                 | Purpose                                                                                                                                         |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `portrait-annotations` | Source annotations such as `@Reflective`, `@Reflective.Include`, `@ProxyTarget`, and `@ProxyTarget.Include` that opt classes into generation.   |
| `portrait-api`         | Unified runtime API (`Portrait.of`, Kotlin extensions, provider registry).                                                                      |
| `portrait-runtime-jvm` | Runtime provider that uses the JVM's reflection for development, testing, and full JDK deployments. No build step required.                     |
| `portrait-codegen`     | CLI that scans compiled classes for opt-ins and emits generated `*$Portrait` types plus metadata blobs.                                         |
| `portrait-runtime-aot` | Runtime provider that loads generated types via `ServiceLoader`, designed for GraalVM Native Image, TeaVM, and similar restricted environments. |

All runtime variants execute the same test suite to guarantee behavior parity.

## Opting Classes Into Portrait

Portrait only reflects the pieces you request. Add the annotations module to both your application and any dependency
that contributes reflective types.

```kotlin
dependencies {
    implementation(libs.portrait.annotations)
}
```

- `@Reflective` on a class turns on generation for that type and its public members.
- `@Reflective.Include` aggregates classes from configuration or build logic without touching their source.
- `@ProxyTarget` on an interface enables proxy generation for public methods.
- `@ProxyTarget.Include` mirrors `@Reflective.Include` for proxies.

Portrait intentionally scopes metadata to **public** constructors, fields, and methods. Private members remain
inaccessible so the generated code stays compatible with restrictive runtimes.

## Generating AoT Metadata

`portrait-codegen` scans your compiled classes, follows opt-in annotations (including nested `Includes`), and emits
matching `*$Portrait` classes and metadata bundles. Generated providers use integer identifiers and switch tables for O(
1) dispatch and register themselves through `META-INF/services/tech.kaffa.portrait.provider.PortraitProvider`, so
`ServiceLoader` works even where reflection is not allowed.

Run the generator as part of your build, then package the outputs alongside `portrait-runtime-aot` for restricted
deployments.

### Typical Workflow

1. Compile sources with `portrait-annotations` available.
2. Execute `portrait-codegen` against the compilation output. Point `--input` at the classpath of compiled classes (
   directories and JARs separated with the platform path separator, for example `dir1;dir2.jar` on Windows or
   `dir1:dir2.jar` on Linux/macOS).
3. Publish or bundle the generated artifacts with your application.
4. On JVM deployments, depend on `portrait-runtime-jvm` for zero-effort reflection.
5. On GraalVM Native Image, TeaVM, or similar environments, swap to `portrait-runtime-aot` and include the generated
   classes. No changes are required in application code.

Because the AoT runtime is wired through `ServiceLoader`, Portrait keeps working on platforms that prohibit dynamic
class loading or reflection.

## portrait-codegen CLI Reference

```
Usage: portrait-codegen --input <path> --output <path> [options]

Portrait AOT Code Generator

Options:
  -i, --input <path>      Input classpath containing compiled classes to scan. Accepts a platform-specific
                          classpath string (directories and/or JARs separated by the system path separator).
  -o, --output <path>     Output file or directory for generated Portrait classes. The format defaults to
                          JAR when the path ends with `.jar`, otherwise a folder.
  -f, --format <value>    Force the output format (`jar` or `folder`), overriding auto-detection.
      --runtime-jar <p>   Add a runtime JAR to the class library search path (repeatable).
      --runtime-dir <p>   Add a directory of runtime `.class` files (repeatable).
      --jre, --jdk <p>    Add a JRE/JDK home whose modules should be visible to the generator (repeatable).
                          Paths are validated to contain a `release` file or `lib/modules`.
      --teavm             Include the embedded TeaVM class library for signature resolution.
  -v, --verbose           Print every discovered reflective class and proxy target.
  -h, --help              Show command help.
```

`--runtime-jar`, `--runtime-dir`, and `--jre/--jdk` augment the generator's understanding of the target runtime class
library. When unspecified, Portrait falls back to the current JVM. Use `--teavm` to bundle the built-in TeaVM remapped
classlib so the generator can resolve TeaVM-specific JDK substitutes.

The tool prints a banner, scans the input classpath, warns about user-provided `PortraitProvider` implementations,
generates all `*$Portrait` types plus proxy handlers, and writes them to the requested location.

## Runtime Selection

- For development and testing on a full JVM, use `portrait-runtime-jvm`. It does not require generated code and relies
  on the platform reflection APIs for quick iteration.
- For GraalVM Native Image, TeaVM, or any environment where reflection is blocked or expensive, use
  `portrait-runtime-aot` and ship the generated artifacts. The runtime loads the generated providers via
  `ServiceLoader`, so it works consistently across restricted platforms.

Both runtimes share the same API surface and participate in the same automated tests, ensuring that switching between
them does not change observable behavior.

## Testing

Portrait exercises both runtimes with shared suites, plus end-to-end tests (`./gradlew :tests-e2e:test`) that validate
sealed hierarchies, proxies, enums, generics, nullability, and multi-constructor classes. TeaVM scenarios execute with
`portrait-runtime-aot`, confirming compatibility with JavaScript/WASM builds.

## Next Steps

1. Annotate your types with `@Reflective` and `@ProxyTarget` (plus their `.Include` variants).
2. Integrate `portrait-codegen` into your build to emit generated companions.
3. Run your application against `portrait-runtime-jvm` locally and `portrait-runtime-aot` in restricted targets without
   changing source code.
