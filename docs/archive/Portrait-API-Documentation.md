# Portrait Public API Class Structure

Portrait is an AOT (Ahead-of-Time) reflection library for Java and Kotlin that provides a unified API for reflective
operations across different platforms. This document outlines the public API class structure.

## Core API Classes (`portrait-api`)

### Portrait (Entry Point)

**Location:** `tech.kaffa.portrait.Portrait`

The primary entry point for the Portrait reflection API. Provides static methods to create PClass instances from various
sources.

**Key Methods:**

- `of(Class<T>)` - Create PClass from Java Class
- `of(KClass<T>)` - Create PClass from Kotlin KClass
- `from(T)` - Create PClass from object instance
- `forName(String)` - Create PClass by class name
- `forNameOrUnresolved(String)` - Create PClass or UnresolvedPClass if not found
- `forPrimitive(String)` - Get PClass for primitive types
- Primitive type convenience methods: `booleanClass()`, `intClass()`, etc.

### PClass&lt;T&gt; (Class Representation)

**Location:** `tech.kaffa.portrait.PClass`

Abstract representation of a class that provides controlled access to reflection capabilities.

**Properties:**

- `simpleName: String` - Simple class name (without package)
- `qualifiedName: String?` - Fully qualified class name
- `isAbstract: Boolean` - True if abstract class
- `isSealed: Boolean` - True if Kotlin sealed class
- `isData: Boolean` - True if Kotlin data class
- `isCompanion: Boolean` - True if Kotlin companion object
- `objectInstance: T?` - Singleton instance for Kotlin objects
- `superclass: PClass<*>?` - Direct superclass
- `interfaces: List<PClass<*>>` - Implemented interfaces
- `annotations: List<PAnnotation>` - Class annotations
- `constructors: List<PConstructor<T>>` - Available constructors
- `declaredMethods: List<PMethod>` - Declared methods
- `declaredFields: List<PField>` - Declared fields

**Key Methods:**

- `createInstance(vararg args: Any?)` - Create new instance
- `isAssignableFrom(PClass<*>)` - Check type compatibility
- `isSubclassOf(PClass<*>)` - Check inheritance
- `getAnnotation(PClass<*>)` - Get specific annotation
- `hasAnnotation(PClass<*>)` - Check annotation presence
- `getConstructor(vararg PClass<*>)` - Get constructor by parameter types
- `getDeclaredMethod(String, vararg PClass<*>)` - Get method by signature
- `getDeclaredField(String)` - Get field by name
- `createProxy(ProxyHandler<T>)` - Create dynamic proxy

### PMethod (Method Representation)

**Location:** `tech.kaffa.portrait.PMethod`

Abstract representation of a method with reflection capabilities.

**Properties:**

- `name: String` - Method name
- `parameterTypes: List<PClass<*>>` - Parameter types
- `parameterCount: Int` - Number of parameters
- `returnType: PClass<*>` - Return type
- `declaringClass: PClass<*>` - Declaring class
- Visibility: `isPublic`, `isPrivate`, `isProtected`
- Modifiers: `isStatic`, `isFinal`, `isAbstract`
- `annotations: List<PAnnotation>` - Method annotations
- `parameterAnnotations: List<List<PAnnotation>>` - Parameter annotations

**Key Methods:**

- `invoke(Any?, vararg Any?)` - Invoke method
- `getAnnotation(PClass<out Annotation>)` - Get annotation
- `hasAnnotation(PClass<out Annotation>)` - Check annotation
- `isCallableWith(vararg PClass<*>)` - Check parameter compatibility
- `matches(String, vararg PClass<*>)` - Check method signature

### PField (Field Representation)

**Location:** `tech.kaffa.portrait.PField`

Abstract representation of a field with reflection capabilities.

**Properties:**

- `name: String` - Field name
- `type: PClass<*>` - Field type
- `declaringClass: PClass<*>` - Declaring class
- Visibility: `isPublic`, `isPrivate`, `isProtected`
- Modifiers: `isStatic`, `isFinal`
- `annotations: List<PAnnotation>` - Field annotations

**Key Methods:**

- `get(Any?)` - Get field value
- `set(Any?, Any?)` - Set field value
- `getAnnotation(PClass<out Annotation>)` - Get annotation
- `hasAnnotation(PClass<out Annotation>)` - Check annotation

### PConstructor&lt;T&gt; (Constructor Representation)

**Location:** `tech.kaffa.portrait.PConstructor`

Abstract representation of a constructor with reflection capabilities.

**Properties:**

- `declaringClass: PClass<T>` - Declaring class
- `parameterTypes: List<PClass<*>>` - Parameter types
- Visibility: `isPublic`, `isPrivate`, `isProtected`
- `annotations: List<PAnnotation>` - Constructor annotations

**Key Methods:**

- `call(vararg Any?)` - Invoke constructor
- `callBy(List<Any?>)` - Invoke with argument list
- `getAnnotation(PClass<out Annotation>)` - Get annotation
- `hasAnnotation(PClass<out Annotation>)` - Check annotation
- `isCallableWith(vararg PClass<*>)` - Check parameter compatibility

### PAnnotation (Annotation Representation)

**Location:** `tech.kaffa.portrait.PAnnotation`

Abstract representation of an annotation with value access.

**Properties:**

- `annotationClass: PClass<out Annotation>` - Annotation type
- `simpleName: String` - Simple annotation name
- `qualifiedName: String?` - Fully qualified annotation name

**Key Methods:**

- `getValue(String)` - Get annotation property value
- Type-specific getters: `getStringValue()`, `getBooleanValue()`, `getIntValue()`, `getListValue()`, `getClassValue()`,
  `getClassListValue()`

### ProxyHandler&lt;T&gt; (Proxy Invocation Handler)

**Location:** `tech.kaffa.portrait.proxy.ProxyHandler`

Functional interface for handling method invocations on dynamic proxies.

**Method:**

- `invoke(T, PMethod, Array<out Any?>?)` - Handle proxy method invocation

### PortraitProvider (Service Provider Interface)

**Location:** `tech.kaffa.portrait.provider.PortraitProvider`

Service provider interface for platform-specific implementations.

**Methods:**

- `priority(): Int` - Provider priority (higher = preferred)
- `forName(String): PClass<T>?` - Resolve class name to PClass

### Extension Properties

**Location:** `tech.kaffa.portrait.Extensions`

Convenient extension properties for fluent API usage:

- `Class<T>.portrait: PClass<T>` - Java Class to PClass
- `KClass<T>.portrait: PClass<T>` - Kotlin KClass to PClass
- `T.portrait: PClass<T>` - Object instance to PClass

### Exceptions

**Location:** `tech.kaffa.portrait.PortraitNotFoundException`, `tech.kaffa.portrait.ProxyCreationException`

- `PortraitNotFoundException` - Thrown when class resolution fails
- `ProxyCreationException` - Thrown when proxy creation fails

## Annotations (`portrait-annotations`)

### @Reflective

**Location:** `tech.kaffa.portrait.Reflective`

Marks a class as requiring reflection capabilities at runtime.

**Parameters:**

- `includeSubclasses: Boolean = false` - Whether to include all subclasses/implementations

**Applicable to:** Classes, interfaces, enums

### @ProxyTarget

**Location:** `tech.kaffa.portrait.ProxyTarget`

Marks an interface as a target for proxy generation.

**Parameters:**

- `includeSubinterfaces: Boolean = false` - Whether to include sub-interfaces

**Applicable to:** Interfaces only

### @OptInPortraits

**Location:** `tech.kaffa.portrait.OptInPortraits`

Allows a class to opt-in other classes for Portrait capabilities without modifying them.

**Parameters:**

- `reflectives: Array<KClass<*>> = []` - Classes to treat as @Reflective
- `proxyTargets: Array<KClass<*>> = []` - Interfaces to treat as @ProxyTarget

**Applicable to:** Classes

## Usage Examples

### Basic Reflection

```kotlin
// Get class representation
val stringClass = String::class.portrait
val pClass = Portrait.of(String::class.java)

// Create instances
val instance = stringClass.createInstance()

// Access methods and fields
val methods = stringClass.declaredMethods
val length = stringClass.getDeclaredMethod("length")
```

### Dynamic Proxies

```kotlin
interface Calculator {
    fun add(a: Int, b: Int): Int
}

val calculatorClass = Calculator::class.portrait
val proxy = calculatorClass.createProxy { self, method, args ->
    when (method.name) {
        "add" -> (args[0] as Int) + (args[1] as Int)
        else -> throw UnsupportedOperationException()
    }
}
```

### Annotations

```kotlin
@Reflective(includeSubclasses = true)
class MyService

@ProxyTarget
interface MyInterface

@OptInPortraits(
    reflectives = [ThirdPartyClass::class],
    proxyTargets = [ExternalInterface::class]
)
class Configuration
```

## Architecture

Portrait uses a **ServiceLoader pattern** for provider discovery, enabling different runtime implementations:

- **JVM Mode**: Delegates to `java.lang.reflect` and `kotlin-reflect`
- **AOT Mode**: Uses build-time generated code for reflection operations

The same application code works in both modes without modification, providing true cross-platform reflection
capabilities.