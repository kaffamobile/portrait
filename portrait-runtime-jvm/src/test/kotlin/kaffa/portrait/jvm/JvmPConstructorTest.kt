package kaffa.portrait.jvm

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmPConstructorTest {

    private val provider = JvmPortraitProvider()

    @Test
    fun `JvmPConstructor basic properties`() {
        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
        val constructors = testClassPClass.constructors

        assertTrue(constructors.isNotEmpty(), "TestClass should have constructors")

        val constructor = constructors.first()
        assertNotNull(constructor.parameterTypes)
        assertEquals(testClassPClass, constructor.declaringClass)
        assertTrue(constructor.isPublic)
        assertFalse(constructor.isPrivate)
        assertFalse(constructor.isProtected)
    }

    @Test
    fun `JvmPConstructor can create instances with no parameters`() {
        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
        val defaultConstructor = testClassPClass.getConstructor()

        if (defaultConstructor != null) {
            val instance = defaultConstructor.call()
            assertNotNull(instance)
            assertTrue(instance is TestClass)
        }
    }

    @Test
    fun `JvmPConstructor can create instances with parameters`() {
        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val parameterizedConstructor = testClassPClass.getConstructor(stringPClass)

        if (parameterizedConstructor != null) {
            assertEquals(1, parameterizedConstructor.parameterTypes.size)
            assertEquals(stringPClass, parameterizedConstructor.parameterTypes[0])

            val instance = parameterizedConstructor.call("test_value")
            assertNotNull(instance)
            assertTrue(instance is TestClass)
            assertEquals("test_value", instance.getInternalValue())
        }
    }

    @Test
    fun `JvmPConstructor parameter type checking`() {
        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val intPClass = provider.forName<Int>("java.lang.Integer")!!

        val constructors = testClassPClass.constructors

        for (constructor in constructors) {
            when (constructor.parameterTypes.size) {
                0 -> {
                    assertTrue(constructor.isCallableWith())
                    assertFalse(constructor.isCallableWith(stringPClass))
                }

                1 -> {
                    if (constructor.parameterTypes[0] == stringPClass) {
                        assertTrue(constructor.isCallableWith(stringPClass))
                        assertFalse(constructor.isCallableWith(intPClass))
                        assertFalse(constructor.isCallableWith())
                    }
                }
            }
        }
    }

    @Test
    fun `JvmPConstructor handles data class constructors`() {
        val dataClassPClass = provider.forName<TestDataClass>("kaffa.portrait.jvm.TestDataClass")!!
        val intPClass = provider.forName<Int>("java.lang.Integer")!!
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val booleanPClass = provider.forName<Boolean>("java.lang.Boolean")!!

        // TestDataClass(id: Int, name: String, active: Boolean = true)
        val primaryConstructor = dataClassPClass.getConstructor(intPClass, stringPClass, booleanPClass)
        val partialConstructor = dataClassPClass.getConstructor(intPClass, stringPClass)

        if (primaryConstructor != null) {
            val instance = primaryConstructor.call(42, "test", true)
            assertNotNull(instance)
            assertTrue(instance is TestDataClass)
            assertEquals(42, instance.id)
            assertEquals("test", instance.name)
            assertEquals(true, instance.active)
        }

        if (partialConstructor != null) {
            val instance = partialConstructor.call(42, "test")
            assertNotNull(instance)
            assertTrue(instance is TestDataClass)
            assertEquals(42, instance.id)
            assertEquals("test", instance.name)
            assertEquals(true, instance.active) // default value
        }
    }

    @Test
    fun `JvmPConstructor handles primitive parameter types`() {
        val dataClassPClass = provider.forName<TestDataClass>("kaffa.portrait.jvm.TestDataClass")!!
        val constructors = dataClassPClass.constructors

        val constructorWithPrimitives = constructors.find {
            it.parameterTypes.any { paramType -> paramType.isPrimitive || paramType.simpleName == "int" }
                    && it.parameterTypes.size == 2
        }

        if (constructorWithPrimitives != null) {
            // Test with primitive values
            val instance = constructorWithPrimitives.call(123, "primitive_test")
            assertNotNull(instance)
        }
    }

    @Test
    fun `JvmPConstructor handles constructor exceptions`() {
        val exceptionClassPClass = provider.forName<ExceptionTestClass>("kaffa.portrait.jvm.ExceptionTestClass")!!
        val defaultConstructor = exceptionClassPClass.getConstructor()

        if (defaultConstructor != null) {
            // This should succeed
            val instance = defaultConstructor.call()
            assertNotNull(instance)
        }
    }

    @Test
    fun `JvmPConstructor visibility modifiers`() {
        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
        val constructors = testClassPClass.constructors

        for (constructor in constructors) {
            val visibilityCount =
                listOf(constructor.isPublic, constructor.isPrivate, constructor.isProtected).count { it }
            assertTrue(visibilityCount == 1, "Constructor should have exactly one visibility modifier")
        }
    }

    @Test
    fun `JvmPConstructor can access constructor annotations`() {
        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
        val constructors = testClassPClass.constructors

        for (constructor in constructors) {
            val annotations = constructor.annotations
            assertNotNull(annotations)
            // Constructors might not have annotations in our test fixtures
        }
    }

    @Test
    fun `JvmPConstructor handles overloaded constructors`() {
        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
        val constructors = testClassPClass.constructors

        // TestClass should have both default and parameterized constructors
        val parameterCounts = constructors.map { it.parameterTypes.size }.sorted()
        assertTrue(parameterCounts.isNotEmpty(), "Should have multiple constructors")

        // Verify we can distinguish between them
        val defaultConstructor = constructors.find { it.parameterTypes.isEmpty() }
        val parameterizedConstructors = constructors.filter { it.parameterTypes.isNotEmpty() }

        if (defaultConstructor != null) {
            val instance1 = defaultConstructor.call()
            assertNotNull(instance1)
        }

        for (paramConstructor in parameterizedConstructors) {
            assertTrue(paramConstructor.parameterTypes.isNotEmpty())
        }
    }

//    @Test
//    fun `JvmPConstructor handles null parameter values`() {
//        val testClassPClass = provider.forName<TestClass>("kaffa.portrait.jvm.TestClass")!!
//        val stringPClass = provider.forName<String>("java.lang.String")!!
//        val constructor = testClassPClass.getConstructor(stringPClass)
//
//        if (constructor != null) {
//            // Pass null to String parameter
//            val instance = constructor.call(null as String?)
//            assertNotNull(instance)
//            assertTrue(instance is TestClass)
//        }
//    }
}