# Portrait: Core Concepts and Architecture

This document outlines the fundamental concepts, design patterns, and architectural principles that make Portrait work
as a cross-platform reflection library.

## Table of Contents

1. [Core Architectural Principles](#core-architectural-principles)
2. [Multi-Platform Reflection Strategy](#multi-platform-reflection-strategy)
3. [Design Patterns](#design-patterns)
4. [Performance Optimization Techniques](#performance-optimization-techniques)
5. [Code Generation Architecture](#code-generation-architecture)
6. [Service Provider System](#service-provider-system)
7. [Annotation-Driven Configuration](#annotation-driven-configuration)
8. [Runtime vs Compile-Time Strategies](#runtime-vs-compile-time-strategies)
9. [Cross-Platform Compatibility](#cross-platform-compatibility)
10. [Error Handling Patterns](#error-handling-patterns)
11. [Build System Architecture](#build-system-architecture)
12. [Testing Strategies](#testing-strategies)

## Core Architectural Principles

### Unified API Abstraction

Portrait's primary goal is to provide a single API that works across different runtime environments:

- **JVM**: Full reflection capabilities using `java.lang.reflect`
- **AOT Compilation**: Pre-generated reflection code for restricted environments
- **TeaVM/WASM**: JavaScript/WebAssembly with no native reflection
- **GraalVM Native Image**: Limited reflection with compile-time analysis
- **Android R8/ProGuard**: Optimized builds with minimal reflection footprint

### Write Once, Reflect Anywhere

The same application code works without modification across all supported platforms by abstracting platform-specific
reflection mechanisms behind a common interface.

## Multi-Platform Reflection Strategy

### JVM Mode (Runtime Reflection)

```kotlin
// Uses standard Java reflection
val method = clazz.getDeclaredMethod("methodName")
method.invoke(instance, args)
```

### AOT Mode (Generated Code)

```kotlin
// Generated at build time
when (methodId) {
    1 -> instance.methodName(args[0] as Type1, args[1] as Type2)
    2 -> instance.otherMethod()
    else -> throw NoSuchMethodException()
}
```

The transition between modes is transparent to application code.

## Design Patterns

### 1. Service Provider Pattern

Central to Portrait's architecture is Java's ServiceLoader mechanism for discovering platform-specific implementations:

```kotlin
// Automatic provider discovery
private val providers: List<PortraitProvider> by lazy {
    ServiceLoader.load(PortraitProvider::class.java).toList()
        .sortedByDescending { it.priority() }
}
```

**Provider Priority System:**

- `WellKnownPortraitProvider`: 200 (Java built-ins: primitives, Object, etc.)
- `JvmPortraitProvider`: 100 (Standard JVM reflection)
- `GeneratedPortraitProvider`: 150 (AOT-generated implementations)
- Custom providers: 0-299 (User-defined implementations)

### 2. Abstract Factory Pattern

`PortraitProvider` serves as an abstract factory for creating platform-specific `PClass` instances:

```kotlin
interface PortraitProvider {
    fun priority(): Int
    fun <T : Any> forName(className: String): PClass<T>?
}
```

### 3. Template Method Pattern

`PClass<T>` defines the reflection API contract while allowing platform-specific implementations:

```kotlin
abstract class PClass<T : Any> {
    abstract val simpleName: String
    abstract fun createInstance(vararg args: Any?): T
    abstract fun getDeclaredMethod(name: String, vararg parameterTypes: PClass<*>): PMethod?
    // ... other abstract methods
}
```

### 4. Strategy Pattern

Different reflection strategies are encapsulated in providers:

- **JvmPortraitProvider**: Delegates to Java reflection
- **GeneratedPortraitProvider**: Uses pre-generated code
- **WellKnownPortraitProvider**: Hardcoded implementations for fundamental types

### 5. Proxy Pattern

Dynamic proxy creation through functional interfaces:

```kotlin
fun interface ProxyHandler<T> {
    fun invoke(self: T, method: PMethod, args: Array<out Any?>?): Any?
}

// Usage
val proxy = interfaceClass.createProxy { self, method, args ->
    // Custom implementation
}
```

### 6. Lazy Loading Pattern

Prevents circular dependencies in type resolution:

```kotlin
override val superclass: PClass<*>? by lazy {
    val superclassName = classEntry.superclassName ?: return@lazy null
    Portrait.forNameOrUnresolved(superclassName)
}
```

## Performance Optimization Techniques

### 1. Sophisticated Caching with Circular Dependency Prevention

```kotlin
private val cache = ConcurrentHashMap<String, PClass<*>>()
private val loadingMarker = object : PClass<Any>() { /* sentinel */ }

private fun <T : Any> load(className: String): PClass<T>? {
    val cached = cache[className]
    if (cached === loadingMarker) {
        throw RuntimeException("Circular dependency detected: $className")
    }
    // ... caching logic
}
```

**Features:**

- Thread-safe concurrent access
- Sentinel values detect circular dependencies
- Cache clearing for testing scenarios

### 2. ID-Based Dispatch for O(1) Method Lookup

Instead of string-based method lookup, generated code uses integer IDs:

```kotlin
// Generated bytecode uses table switching
when (methodId) {
    0 -> target.toString()
    1 -> target.hashCode()
    2 -> target.equals(args[0])
    // ...
}
```

This generates efficient bytecode with `tableswitch` instructions for O(1) method dispatch.

### 3. String Pooling in Metadata

Custom binary format with string deduplication:

```kotlin
// Metadata serialization reduces duplication
const val MAGIC_NUMBER = 0x504D4144 // "PMAD" - Portrait Metadata
const val VERSION = 1

// Base85 encoding for string representation
// String pooling reduces file size
```

### 4. Lazy Provider Discovery

Providers are discovered only when first needed, reducing startup time:

```kotlin
private val providers: List<PortraitProvider> by lazy {
    // ServiceLoader discovery happens on first access
}
```

## Code Generation Architecture

### 1. Multi-Phase Generation Process

#### Phase 1: Classpath Scanning

```kotlin
// Uses ClassGraph + ASM for efficient scanning
val classGraph = ClassGraph()
    .overrideClasspath(classpath)
    .enableAllInfo()
val results = classGraph.scan()
```

#### Phase 2: Annotation Processing

```kotlin
val reflectives = findReflectives(results)
val proxyTargets = findProxyTargets(results)
val optIns = findOptInPortraits(results)
```

#### Phase 3: Code Generation

Uses ByteBuddy for dynamic class creation:

```kotlin
byteBuddy.subclass(
    TypeDescription.Generic.Builder
        .parameterizedType(StaticPortrait::class.java, targetType)
        .build()
)
```

### 2. Generated Class Structure

Each annotated class gets a corresponding `*$Portrait` class:

```kotlin
// For class com.example.MyClass
// Generates: com.example.MyClass$Portrait extends StaticPortrait<MyClass>

class MyClass$Portrait : StaticPortrait<MyClass> {
    override fun invokeMethod(target: MyClass, methodId: Int, args: Array<Any?>): Any? {
        return when (methodId) {
            0 -> target.method1()
            1 -> target.method2(args[0] as String)
            // ...
        }
    }
}
```

### 3. Metadata Serialization

Binary format for type information:

```kotlin
// Compact binary format with:
-Magic number and version
        -String pool for deduplication
-Bit flags for boolean properties
        -Variable - length encoding for integers
-Base85 encoding for string representation
```

## Service Provider System

### Provider Registration

Providers register via `META-INF/services/kaffa.portrait.provider.PortraitProvider`:

```
kaffa.portrait.runtime.jvm.JvmPortraitProvider
kaffa.portrait.internal.WellKnownPortraitProvider
com.example.CustomPortraitProvider
```

### Provider Chain Resolution

When resolving a class name:

1. Check cache first
2. Try providers in priority order (highest first)
3. Return first successful result
4. Cache result for future use
5. Return null if no provider can handle it

### Graceful Degradation

```kotlin
fun forNameOrUnresolved(className: String): PClass<*> {
    return load<Any>(className) ?: UnresolvedPClass<Any>(className)
}
```

System continues operating even when some types are unavailable.

## Annotation-Driven Configuration

### Core Annotations

#### @Reflective

Marks classes for reflection capabilities:

```kotlin
@Reflective(includeSubclasses = true)
class MyService
```

#### @ProxyTarget

Marks interfaces for proxy generation:

```kotlin
@ProxyTarget(includeSubinterfaces = true)
interface MyInterface
```

#### @OptInPortraits

Enables third-party class reflection:

```kotlin
@OptInPortraits(
    reflectives = [ThirdPartyClass::class, LegacyService::class],
    proxyTargets = [ExternalInterface::class]
)
class Configuration
```

### Transitive Inclusion

Annotations support automatic inclusion of related types:

- `includeSubclasses`: All subclasses get reflection
- `includeSubinterfaces`: All sub-interfaces get proxy support

## Runtime vs Compile-Time Strategies

### Runtime Strategy (JVM)

**Advantages:**

- Full dynamic capabilities
- No build-time configuration required
- Works with any class at runtime

**Trade-offs:**

- Higher runtime overhead
- Larger memory footprint
- Potential for reflection security issues

### Compile-Time Strategy (AOT)

**Advantages:**

- Minimal runtime overhead
- Smaller memory footprint
- Works in restricted environments
- Better for static analysis tools

**Trade-offs:**

- Limited to pre-declared classes
- Requires build-time configuration
- Less flexibility for dynamic scenarios

### Hybrid Approach

Portrait allows mixing both strategies:

- AOT for known, performance-critical classes
- JVM reflection for dynamic scenarios
- Automatic fallback between strategies

## Cross-Platform Compatibility

### Platform-Specific Challenges

#### TeaVM (JavaScript/WebAssembly)

- No native reflection support
- Solution: Generate JavaScript-compatible code
- Maintain same API through code generation

#### GraalVM Native Image

- Limited reflection support
- Solution: Pre-register reflection metadata
- Use generated code where possible

#### Android R8/ProGuard

- Aggressive code shrinking and obfuscation
- Solution: Keep rules for Portrait annotations
- Generate optimized code paths

### Abstraction Layers

Portrait provides multiple abstraction layers:

1. **API Layer**: Common interface (`PClass`, `PMethod`, etc.)
2. **Provider Layer**: Platform-specific implementations
3. **Runtime Layer**: Environment detection and adaptation
4. **Generation Layer**: Build-time code and metadata generation

## Error Handling Patterns

### Fail-Fast vs Graceful Degradation

Portrait uses different strategies based on context:

#### Fail-Fast (Application Code)

```kotlin
// Throws PortraitNotFoundException for missing types
val pClass = Portrait.forName("com.example.MyClass")
```

#### Graceful Degradation (Internal Resolution)

```kotlin
// Returns UnresolvedPClass to allow continued operation
val pClass = Portrait.forNameOrUnresolved("com.example.MaybeExistentClass")
```

### Provider Error Handling

Providers return `null` for non-handleable cases rather than throwing exceptions:

```kotlin
// Good provider implementation
fun <T : Any> forName(className: String): PClass<T>? {
    return try {
        if (canHandle(className)) createPClass(className) else null
    } catch (e: Exception) {
        null // Don't throw, let other providers try
    }
}
```

### Circular Dependency Detection

Runtime detection with meaningful error messages:

```kotlin
if (cached === loadingMarker) {
    throw RuntimeException("Circular dependency detected while loading class: $className")
}
```

## Build System Architecture

### Multi-Module Structure

```
portrait/
├── portrait-annotations/    # Core annotations (@Reflective, @ProxyTarget, @OptInPortraits)
├── portrait-api/           # Abstract API (PClass, PMethod, PField, Portrait)
├── portrait-runtime-jvm/   # JVM implementation using java.lang.reflect
├── portrait-runtime-aot/   # AOT runtime support (StaticPortrait base classes)
├── portrait-codegen/       # Build-time code generation tools
└── build.gradle.kts        # Root build configuration
```

### Dependency Graph

```
portrait-annotations (no dependencies)
        ↑
portrait-api (depends on: annotations)
        ↑
├── portrait-runtime-jvm (depends on: api, kotlin-reflect)
├── portrait-runtime-aot (depends on: api)
└── portrait-codegen (depends on: api, annotations, runtime-aot, asm, bytebuddy)
```

### Key Dependencies

- **Kotlin Standard Library**: Core language support
- **Kotlin Reflection**: JVM runtime reflection
- **ByteBuddy**: Dynamic class generation
- **ClassGraph**: Efficient classpath scanning
- **ASM**: Bytecode manipulation
- **Kotlinx Metadata**: Kotlin metadata processing

### Build Tools Integration

The codegen module can be integrated with build tools:

```kotlin
// Gradle plugin integration
application {
    mainClass.set("kaffa.portrait.codegen.PortraitCodegen")
}
```

## Testing Strategies

### Constraint: No Local Classes

Portrait cannot handle local classes due to classloader limitations:

```kotlin
// ❌ Won't work - local class
fun testLocalClass() {
    class LocalTestClass
    Portrait.of(LocalTestClass::class.java) // Fails
}

// ✅ Works - top-level class
class TopLevelTestClass

fun testTopLevelClass() {
    Portrait.of(TopLevelTestClass::class.java) // Success
}
```

### Test Fixtures Pattern

Comprehensive test classes with various patterns:

```kotlin
@Reflective
class TestClass {
    fun publicMethod() = "public"
    private fun privateMethod() = "private"

    companion object {
        fun staticMethod() = "static"
    }
}

@ProxyTarget
interface TestInterface {
    fun interfaceMethod(): String
}
```

### Provider Testing

Tests verify provider discovery and priority:

```kotlin
@Test
fun testProviderOrdering() {
    val providers = ServiceLoader.load(PortraitProvider::class.java).toList()
    assertTrue(providers.isNotEmpty())
    // Verify priority ordering
}
```

### Circular Dependency Testing

Specialized tests for edge cases:

```kotlin
@Test
fun testCircularDependency() {
    // Test classes that reference each other
    assertThrows<RuntimeException> {
        Portrait.forName("com.example.CircularA")
    }
}
```

## Key Innovation: Reflection Portability

Portrait's core innovation is making reflection portable across environments where reflection may not exist or may be
restricted. This is achieved through:

### 1. API Unification

Same API works everywhere, regardless of underlying implementation.

### 2. Transparent Fallback

Automatic switching between implementations based on environment capabilities.

### 3. Build-Time Analysis

Code generation provides reflection-like capabilities where native reflection is unavailable.

### 4. Performance Optimization

Generated code often outperforms native reflection through specialized optimizations.

### 5. Cross-Platform Compatibility

Single codebase works across JVM, JavaScript, WebAssembly, and native compilation targets.

This architectural approach enables:

- **Gradual Migration**: Start with JVM reflection, migrate to AOT incrementally
- **Deployment Flexibility**: Same code works in different environments
- **Performance Tuning**: Choose optimal strategy per platform
- **Future-Proofing**: Easy adaptation to new platforms and constraints

Portrait represents a sophisticated solution to the challenge of providing consistent reflection capabilities across the
diverse landscape of modern Java-compatible runtime environments.