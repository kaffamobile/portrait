# Portrait E2E Tests

This module contains end-to-end tests for the Portrait reflection library.

## Structure

- **`src/main/kotlin`**: Test fixtures - annotated classes that will be processed by codegen
- **`src/test/kotlin`**: E2E tests that verify Portrait's reflection capabilities

## Running Tests

### JVM Tests (Default)

```bash
./gradlew :tests-e2e:test
```

This runs all tests on the JVM using standard JUnit and Portrait's runtime reflection.

### TeaVM Tests (JavaScript/WASM)

To run tests on TeaVM (JavaScript), you need Java 11+ for the Gradle daemon:

1. **Set `JAVA_HOME` to Java 11+** (or use a tool like `jenv` to switch Java versions)

2. **Uncomment the TeaVM plugin** in `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.teavm)  // <- Uncomment this
    `java-library`
}
```

3. **Add TeaVM configuration** at the end of `build.gradle.kts`:

```kotlin
teavm {
    tests {
        js {
            enabled = true
            // Options: CHROME, FIREFOX, NONE (headless)
            runner = org.teavm.gradle.api.TeaVMTestRunner.CHROME
        }
    }
}
```

4. **Run TeaVM tests**:

```bash
./gradlew :tests-e2e:testJs
```

## Test Fixtures

The test fixtures in `src/main/kotlin` include:

- **`SimpleReflectiveClass`**: Basic data class with methods
- **`Operation` hierarchy**: Sealed class with implementations (`Addition`, `Multiplication`)
- **`Calculator`**: Interface for proxy testing
- **`ServiceClass`**: Service with state and multiple methods
- **`SingletonService`**: Object/singleton testing
- **`Status`**: Enum testing
- **`FieldTestClass`**: Various field types and access patterns
- **`MultiConstructorClass`**: Multiple constructor overloads
- **`Container<T>`**: Generic type handling
- **`NullableTestClass`**: Nullable type handling

All fixtures are annotated with `@Reflective` or `@ProxyTarget` to enable Portrait codegen.

## E2E Tests

The E2E tests verify:

- Class reflection (`Portrait.of()`, `Portrait.forName()`)
- Instance creation (`createInstance()`, constructors)
- Method invocation (with and without parameters)
- Field access (get/set operations)
- Sealed class hierarchies
- Object/singleton instances
- Enum reflection
- Proxy generation and invocation
- Generic types
- Nullable types

All tests are annotated with `@RunWith(TeaVMTestRunner::class)` to ensure TeaVM compatibility.

## Writing New Tests

When adding new tests:

1. **Add test fixtures to `src/main/kotlin`** - these are the classes that codegen will process
2. **Annotate fixtures** with `@Reflective` or `@ProxyTarget`
3. **Write tests in `src/test/kotlin`** using standard JUnit assertions
4. **Annotate test classes** with `@RunWith(TeaVMTestRunner::class)`
5. **Avoid local classes** - always use top-level classes or objects

## Why Two Source Sets?

- **`src/main`**: Contains annotated classes that the codegen processor will analyze and generate `*$Portrait` classes for
- **`src/test`**: Contains actual test code that uses Portrait API to verify both runtime (JVM) and generated (AOT) modes work correctly

This separation ensures the codegen has clean input classes to process.
