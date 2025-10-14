# Portrait Testing Guidelines

## Important Limitations

### Local Classes Not Supported

Portrait does not support local classes (classes defined inside methods or functions). When writing tests:

❌ **Don't do this:**

```kotlin
@Test
fun `some test`() {
    @TestAnnotation
    class LocalClass  // This won't work with Portrait
    
    val portrait = Portrait.of(LocalClass::class.java)
}
```

✅ **Do this instead:**

```kotlin
// Define test classes at top-level
@TestAnnotation
@Reflective
class TopLevelTestClass

@Test
fun `some test`() {
    val portrait = Portrait.of(TopLevelTestClass::class.java)
}
```

### Reasons for this Limitation

- Local classes have complex naming conventions that vary by runtime
- They don't have stable class names that can be resolved reliably
- They exist only within the scope of their enclosing method
- Portrait's service provider mechanism expects stable, fully-qualified class names

### Recommended Patterns

1. **Use TestFixtures**: Define test classes in TestFixtures.kt files
2. **Top-level classes**: Define test classes at the top level of test files
3. **Companion objects**: For singleton-like test objects, use companion objects instead of local classes

## Test Structure

### Module Test Organization

- `portrait-annotations`: Tests for annotation definitions and metadata
- `portrait-api`: Tests for core API functionality and contracts
- `portrait-runtime-jvm`: Tests for JVM-specific implementations
- `portrait-runtime-aot`: Tests for AOT-specific implementations
- `portrait-codegen`: Tests for code generation functionality

### Primitive Type Support

Portrait provides built-in support for Java primitive types:

- `boolean`, `byte`, `char`, `short`, `int`, `long`, `float`, `double`, `void`
- These are handled directly in the Portrait API without requiring providers
- Use `Portrait.forName("boolean")` or `Portrait.booleanClass()` for primitive types

### Circular Dependency Prevention

Portrait implementations use lazy loading to prevent circular dependencies:

- All PClass references in JvmP* and StaticP* classes are lazy-loaded
- Properties like `superclass`, `interfaces`, `parameterTypes`, `returnType`, etc. are evaluated only when accessed
- This prevents StackOverflowErrors when classes reference each other in complex inheritance hierarchies
- Caching in the Portrait object provides additional protection against circular dependencies

### Expected Test Failures

- When running tests without proper provider configuration, expect:
    - `NullPointerException`: Normal when providers can't resolve classes
    - `AssertionFailedError`: Expected when assertions fail due to missing implementations
    - `RuntimeException`: When no providers are available on classpath

- You should NOT see:
    - `StackOverflowError`: Indicates circular dependencies (should be prevented by caching)
    - `ClassNotFoundException: boolean` (or other primitives): Portrait handles primitives directly
    - Local class resolution errors: Portrait doesn't support local classes