# Portrait - AOT Reflection Library Specification

## Overview

Portrait is an AOT (Ahead-of-Time) reflection library for Java and Kotlin that provides a unified API for reflective
operations across different platforms, including JVM, TeaVM (JS/WASM), GraalVM Native Image, and other restricted
environments.

## Motivation

Modern Java/Kotlin applications often need to run on platforms where traditional reflection is unavailable or
restricted:

- **TeaVM**: JavaScript/WASM targets don't support reflection
- **GraalVM Native Image**: Requires explicit registration of reflective access
- **ProGuard/R8**: Aggressive optimization breaks reflection
- **Kotlin Multiplatform**: Different targets have different reflection capabilities

Portrait solves this by generating concrete implementations at build time while providing a familiar API.

## Core Concept

Portrait operates in two modes:

1. **Runtime Mode (JVM)**: Delegates to native `java.lang.reflect` and `kotlin-reflect` APIs
2. **Generated Mode**: Uses build-time generated code for reflective operations

The same application code works in both modes without modification.

## Architecture

```
portrait-annotations/   # @Reflective, @ProxyTarget annotations
portrait-api/          # Public API: PClass, PMethod, PField, etc
portrait-runtime/      # JVM implementation (delegates to native reflection)
portrait-codegen/      # Code generator (bytecode scanning + code generation)
portrait-gradle/       # Gradle plugin
portrait-maven/        # Maven plugin (future)
```

## How It Works

### 1. Marking Classes

Developers annotate classes/interfaces that require reflective access:

```java
@Reflective
public class User {
    private Long id;
    private String name;
    
    public User() {}
    public User(Long id, String name) { this.id = id; this.name = name; }
    
    public void save() {}
    public Long getId() { return id; }
    public void setName(String name) { this.name = name; }
}

@ProxyTarget
public interface MyService {
    boolean execute(String command);
}
```

### 2. Build-Time Code Generation

The Gradle/Maven plugin:

1. Scans compiled bytecode (classes + dependencies) using ASM
2. Finds all `@Reflective` and `@ProxyTarget` annotations
3. Generates `*$Portrait` classes with concrete implementations
4. Compiles generated code alongside application code

### 3. Generated Portrait Class

For each annotated type, Portrait generates a class like:

```kotlin
class User$Portrait : Portrait<User> {

    // Constructor invocation by ID
    override fun newInstance(constructorId: Int, args: Array<Any?>): User {
        return when (constructorId) {
            0 -> User()
            1 -> User(args[0] as Long, args[1] as String)
            else -> throw NoSuchMethodException()
        }
    }

    // Method invocation by ID
    override fun invoke(target: User, methodId: Int, args: Array<Any?>): Any? {
        return when (methodId) {
            0 -> target.save()
            1 -> target.getId()
            2 -> target.setName(args[0] as String)
            else -> throw NoSuchMethodException()
        }
    }

    // Field access by ID
    override fun getField(target: User, fieldId: Int): Any? {
        return when (fieldId) {
            0 -> target.id
            1 -> target.name
            else -> throw NoSuchFieldException()
        }
    }

    override fun setField(target: User, fieldId: Int, value: Any?) {
        when (fieldId) {
            1 -> target.name = value as String
            else -> throw IllegalAccessException()
        }
    }

    // Lookup methods (name + param types -> ID)
    override fun constructorId(paramTypes: Array<Class<*>>): Int = when {
        paramTypes.isEmpty() -> 0
        paramTypes.contentEquals(arrayOf(Long::class.java, String::class.java)) -> 1
        else -> -1
    }

    override fun methodId(name: String, paramTypes: Array<Class<*>>): Int = when {
        name == "save" && paramTypes.isEmpty() -> 0
        name == "getId" && paramTypes.isEmpty() -> 1
        name == "setName" && paramTypes.contentEquals(arrayOf(String::class.java)) -> 2
        else -> -1
    }

    override fun fieldId(name: String): Int = when (name) {
        "id" -> 0
        "name" -> 1
        else -> -1
    }

    // Proxy creation
    override fun createProxy(handler: InvocationHandler): User {
        return User$Proxy(handler)
    }

    private class User$Proxy(private val h: InvocationHandler) : User(0L, "") {
    override fun save() = h.invoke(this, 0, EMPTY) as Boolean
    override fun getId() = h.invoke(this, 1, EMPTY) as Long
    override fun setName(v: String) {
        h.invoke(this, 2, arrayOf(v))
    }
}

    companion object {
    private val EMPTY = emptyArray<Any?>()
}
}
```

### 4. Public API Usage

#### Java API

Mirrors `java.lang.reflect`:

```java
// Get Portrait class
PClass<User> pClass = Portrait.of(User.class);

// Create instance
User user = pClass.getDeclaredConstructor(Long.class, String.class)
        .newInstance(1L, "test");

// Invoke method
PMethod method = pClass.getMethod("save");
method.

invoke(user);

// Access field
PField field = pClass.getField("name");
field.

set(user, "new name");

// Create proxy
MyService proxy = Portrait.newProxyInstance(
        classLoader,
        new Class[]{MyService.class},
        handler
);
```

#### Kotlin API

Mirrors `kotlin.reflect`:

```kotlin
// Get Portrait class
val pClass = User::class.portrait

// Create instance
val user = pClass.createInstance()

// Invoke method
val func = pClass.functions.find { it.name == "save" }
func?.call(user)

// Access property
val prop = pClass.memberProperties.find { it.name == "name" }
prop?.setter?.call(user, "new name")

// Create proxy
val proxy = MyService::class.createProxy(handler)
```

## Migration Path

Portrait is designed for easy migration from native reflection APIs:

**Java:**

- Replace `Class` with `PClass`
- Replace `Method` with `PMethod`
- Replace `Field` with `PField`
- Replace `Proxy.newProxyInstance` with `Portrait.newProxyInstance`

**Kotlin:**

- Replace `::class` with `::class.portrait`
- Rest of the API remains the same

## Technical Decisions

### Generics and Type Erasure

Portrait uses `Array<Class<*>>` for parameter type matching. Since generics are erased at runtime, `List<String>` and
`List<Integer>` are both represented as `List.class`. This is sufficient for method dispatch and matches JVM behavior.

### ID-Based Dispatch

Generated classes use integer IDs for methods, constructors, and fields. This enables efficient `when` statements that
compile to `tableswitch` bytecode (O(1) lookup).

### Bytecode Scanning

The build plugin uses ASM to scan bytecode rather than KSP/KAPT because:

- Works with pure Java code
- Processes compiled dependencies (JARs)
- Faster than KAPT
- No need to handle source-level AST

### Minimal Bytecode Generation

The generated code prioritizes minimal bytecode size over readability:

- Reuses empty arrays (`EMPTY`)
- Uses numeric IDs instead of descriptor objects
- Inline proxy classes within Portrait classes
- Direct method calls instead of intermediate objects

### Runtime Assumptions

Portrait assumes the target platform supports:

- `Class<*>` objects and `.getClass()`
- Basic class comparison (`==`, `contentEquals`)
- Exception throwing

## Scope

### Initial Scope

- Constructor invocation
- Method invocation
- Field read/write
- Method/field/constructor listing
- Proxy creation
- Type information (parameter types, return types)

### Out of Scope (Initial)

- Generic type introspection (signatures)
- Annotations introspection
- Modifiers (public, private, static, etc.)
- Access control enforcement

## Plugin Configuration

The Gradle plugin will scan the classpath for `@Reflective` and `@ProxyTarget` annotations and generate Portrait classes
automatically during compilation.

```kotlin
// build.gradle.kts
plugins {
    id("com.portrait.gradle") version "1.0.0"
}
```

For platforms with native reflection (standard JVM), the plugin is optional. The application will use the runtime
implementation that delegates to native APIs.

## Next Steps

1. Define annotation types (`@Reflective`, `@ProxyTarget`)
2. Implement base `Portrait<T>` interface
3. Implement runtime delegation layer
4. Build bytecode scanner using ASM
5. Implement code generator using KotlinPoet/JavaPoet
6. Create Gradle plugin
7. Add support for additional targets (TeaVM, GraalVM)