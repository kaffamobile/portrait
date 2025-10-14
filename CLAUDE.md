# Portrait AI Assistant Guide

## What is Portrait?

Portrait is an AOT (Ahead-of-Time) reflection library for Java and Kotlin that provides a **unified reflection API**
that works across JVM, TeaVM (JS/WASM), GraalVM Native Image, and other restricted environments.

**Core Innovation**: "Write Once, Reflect Anywhere" - the same application code works in both runtime reflection mode (
JVM) and generated code mode (AOT) without modification.

### Two Operating Modes

1. **Runtime Mode (JVM)**: Delegates to `java.lang.reflect` and `kotlin-reflect`
2. **Generated Mode (AOT)**: Uses build-time generated `*$Portrait` classes for reflective operations

## Project Structure

```
portrait/
├── portrait-annotations/    # @Reflective, @ProxyTarget, @Reflective.Include, @ProxyTarget.Include
├── portrait-api/           # Abstract API (Portrait, PClass, PMethod, PField, PConstructor)
├── portrait-runtime-jvm/   # JVM implementation using java.lang.reflect + kotlin-reflect
├── portrait-runtime-aot/   # AOT runtime support (StaticPortrait base classes)
└── portrait-codegen/       # Build-time code generation (ASM + ByteBuddy)
```

**Dependency Chain**: `annotations` → `api` → {`runtime-jvm`, `runtime-aot`, `codegen`}

## Quick Reference

### Build Commands

```bash
./gradlew build    # Build all modules
./gradlew clean    # Clean artifacts
./gradlew test     # Run tests
./gradlew check    # Verification + lint
```

### Dependency Management

Portrait uses Gradle version catalogs (`gradle/libs.versions.toml`) for centralized dependency management:

- **All dependencies** are managed through the version catalog
- **Never** add inline dependency versions (e.g., `"io.mockk:mockk:1.12.5"`)
- **Always** use catalog references (e.g., `libs.mockk`)
- When adding new dependencies:
    1. Add version to `[versions]` section
    2. Add library definition to `[libraries]` section
    3. Reference using `libs.{name}` in `build.gradle.kts`

**Example:**

```toml
# gradle/libs.versions.toml
[versions]
mockk = "1.12.5"

[libraries]
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
```

```kotlin
// build.gradle.kts
dependencies {
    testImplementation(libs.mockk)  // ✅ CORRECT
    // testImplementation("io.mockk:mockk:1.12.5")  // ❌ WRONG
}
```

### Main Entry Points

**Primary API**: `kaffa.portrait.Portrait`

```kotlin
Portrait.of(MyClass::class.java)           // From Class
Portrait.from(myInstance)                  // From instance
Portrait.forName("com.example.MyClass")    // By name
Portrait.forNameOrUnresolved(name)         // Safe lookup
```

**Extension Properties**: `kaffa.portrait.Extensions`

```kotlin
MyClass::class.portrait                    // KClass → PClass
MyClass::class.java.portrait               // Class → PClass
myInstance.portrait                        // Instance → PClass
```

### Core Abstractions

**`PClass<T>`** - Class representation with:

- Properties: `simpleName`, `qualifiedName`, `isAbstract`, `isSealed`, `isData`, `objectInstance`
- Type relations: `superclass`, `interfaces`, `isAssignableFrom()`, `isSubclassOf()`
- Reflection: `constructors`, `declaredMethods`, `declaredFields`, `annotations`
- Operations: `createInstance()`, `createProxy()`, `getConstructor()`, `getDeclaredMethod()`

**`PMethod`** - Method invocation with `invoke(target, args)`

**`PField`** - Field access with `get(target)`, `set(target, value)`

**`PConstructor<T>`** - Constructor calling with `call(args)`

## Key Annotations

### @Reflective

Marks classes for reflection capabilities.

```kotlin
@Reflective(including = [Includes.ALL_SUBTYPES])
class MyService
```

### @ProxyTarget

Marks interfaces for proxy generation.

```kotlin
@ProxyTarget(including = [Includes.ALL_SUBTYPES])
interface MyInterface
```

### @Reflective.Include

Opts-in third-party classes for reflection without modifying them.

```kotlin
@Reflective.Include(
    classes = [ThirdPartyClass::class],
    including = [Includes.DIRECT_SUBTYPES]
)
class Configuration
```

### @ProxyTarget.Include

Opts-in third-party interfaces for proxy generation without modifying them.

```kotlin
@ProxyTarget.Include(
    classes = [ExternalInterface::class],
    including = [Includes.ALL_SUBTYPES]
)
class Configuration
```

## Architecture Patterns

### Service Provider Pattern

Portrait uses `ServiceLoader` to discover implementations with priority-based resolution:

**Provider Priorities** (higher = preferred):

- `WellKnownPortraitProvider`: 200 (Java built-ins like String, Integer)
- `GeneratedPortraitProvider`: 150 (AOT-generated `*$Portrait` classes)
- `JvmPortraitProvider`: 100 (Standard reflection via `java.lang.reflect`)
- Custom providers: 0-299

### Caching Strategy

Thread-safe concurrent caching with sentinel values to detect circular dependencies:

```kotlin
private val cache = ConcurrentHashMap<String, PClass<*>>()
private val loadingMarker = object : PClass<Any>() { /* sentinel */ }
```

### Lazy Loading

Prevents circular dependencies in type resolution:

```kotlin
override val superclass: PClass<*>? by lazy {
    Portrait.forNameOrUnresolved(classEntry.superclassName ?: return@lazy null)
}
```

### ID-Based Dispatch

Generated code uses integer IDs for O(1) method lookup via table switching:

```kotlin
// In generated *$Portrait classes
override fun invokeMethod(target: MyClass, methodId: Int, args: Array<Any?>): Any? {
    return when (methodId) {
        0 -> target.method1()
        1 -> target.method2(args[0] as String)
        // ... O(1) dispatch instead of string lookup
    }
}
```

## Code Generation Process

### Multi-Phase Generation

1. **Classpath Scanning**: ClassGraph + ASM find annotated classes
2. **Annotation Processing**: Collect @Reflective, @ProxyTarget, @Reflective.Include, @ProxyTarget.Include
3. **Code Generation**: ByteBuddy creates `*$Portrait` classes

### Generated Class Structure

For each `@Reflective class MyClass`, generates `MyClass$Portrait extends StaticPortrait<MyClass>`:

- Implements abstract methods from `StaticPortrait`
- Uses ID-based dispatch for methods/fields/constructors
- Includes serialized metadata for type information

### Metadata Serialization

Binary format with:

- Magic number and version header
- String pool for deduplication
- Bit flags for boolean properties
- Base64 encoding for string representation

## Critical Testing Rules

### ❌ Local Classes Are NOT Supported

Portrait **cannot** reflect on local classes (classes defined inside methods):

```kotlin
// ❌ WRONG - Will fail
@Test
fun someTest() {
    @Reflective
    class LocalClass  // Won't work!
    Portrait.of(LocalClass::class.java)
}

// ✅ CORRECT - Use top-level classes
@Reflective
class TopLevelTestClass

@Test
fun someTest() {
    Portrait.of(TopLevelTestClass::class.java)
}
```

**Why**: Local classes have unstable names, complex scoping, and can't be reliably resolved by ServiceLoader.

### Testing Best Practices

1. Define test classes at **top-level** or in **TestFixtures.kt**
2. Use **companion objects** for singleton-like test objects
3. Never define `@Reflective` classes inside test methods
4. Primitives (`boolean`, `int`, etc.) are handled directly - no special setup needed

### Expected Test Behaviors

- **Circular dependencies**: Prevented by lazy loading + caching
- **Missing providers**: `RuntimeException` when no providers on classpath
- **Unresolved classes**: Returns `UnresolvedPClass` from `forNameOrUnresolved()`
- **Provider failures**: May throw `NullPointerException` or `AssertionFailedError`

### Red Flags

- `StackOverflowError`: Indicates circular dependency bug (shouldn't happen with proper caching)
- `ClassNotFoundException: boolean`: Portrait handles primitives - this indicates a bug

## Development Guidelines

### Code Patterns to Follow

- Use `@Reflective` or `@Reflective.Include` for reflection support
- Test classes must be **top-level**, never local
- Use lazy loading for `PClass` references to prevent circular dependencies
- Follow existing patterns in each module

### Codegen Module Restriction

**In `portrait-codegen`, you CANNOT use Portrait API or Portrait Runtime AOT** to load/manipulate classes, because
codegen doesn't load the classes it's generating code for into its classpath.

**Exception**: You CAN reference types like `PClass::class.java` for metadata purposes.

### Cross-Platform Targets

Portrait enables deployment to:

- **TeaVM** (JS/WASM): No native reflection → generated code provides capabilities
- **GraalVM Native Image**: Limited reflection → pre-generated metadata + code
- **ProGuard/R8**: Code shrinking → optimized code paths + keep rules
- **Kotlin Multiplatform**: Share reflection code across platforms

### Common Use Cases Portrait Replaces

- `Class.forName()` for dynamic class loading
- `.getMethod()/.invoke()` for dynamic method calls
- `.getAnnotation()` for annotation processing
- `org.reflections` library for classpath scanning
- Kotlin `.objectInstance` and `.createInstance()` patterns

## Troubleshooting

| Issue                                   | Cause                     | Solution                                                               |
|-----------------------------------------|---------------------------|------------------------------------------------------------------------|
| Missing reflection support              | Class not annotated       | Add `@Reflective` or use `@Reflective.Include`                         |
| `RuntimeException: No PortraitProvider` | Provider not registered   | Ensure provider in `META-INF/services/kaffa.portrait.provider.PortraitProvider` |
| Local class resolution fails            | Local classes unsupported | Use top-level classes or TestFixtures                                  |
| Circular dependency errors              | Missing lazy loading      | Portrait handles automatically via caching - shouldn't occur           |

## Current Project Status

- **Branch**: master
- **Version**: 1.0.0-SNAPSHOT
- **Complete**: annotations, api, runtime-jvm (partial)
- **In Progress**: codegen, runtime-aot

## Key Dependencies

All dependencies are centrally managed in `gradle/libs.versions.toml`:

**Core Dependencies:**

- **Kotlin Standard Library** + **kotlin-reflect**: Core language + JVM reflection
- **ByteBuddy**: Dynamic class generation
- **ClassGraph**: Efficient classpath scanning
- **ASM**: Bytecode manipulation
- **Kotlinx Metadata**: Kotlin metadata processing

**Testing Dependencies:**

- **JUnit Jupiter**: Test framework
- **MockK**: Kotlin mocking library

**Logging Dependencies (codegen only):**

- **SLF4J API**: Logging facade
- **Logback Classic**: Logging implementation

## Why Portrait Matters

Portrait's innovation is **reflection portability** - providing consistent reflection capabilities across environments
where reflection doesn't exist or is restricted. This enables:

1. **API Unification**: Same API everywhere, regardless of implementation
2. **Transparent Fallback**: Automatic switching between runtime/generated modes
3. **Build-Time Analysis**: Generated code provides reflection-like capabilities
4. **Performance**: Generated code often outperforms native reflection
5. **Future-Proofing**: Easy adaptation to new platforms and constraints

**Result**: Gradual migration paths, deployment flexibility, and cross-platform compatibility for reflection-heavy
applications.