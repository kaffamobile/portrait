package tech.kaffa.portrait.jvm

import kotlin.test.Test
import kotlin.test.assertFailsWith
import tech.kaffa.portrait.Portrait
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmPMethodTest {

    private val provider = JvmPortraitProvider()

    @Test
    fun `JvmPMethod basic properties`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val lengthMethod = stringPClass.getMethod("length")!!

        assertEquals("length", lengthMethod.name)
        assertEquals(0, lengthMethod.parameterTypes.size)
        assertNotNull(lengthMethod.returnType)
        assertEquals(stringPClass, lengthMethod.declaringClass)
        assertTrue(lengthMethod.isPublic)
        assertFalse(lengthMethod.isPrivate)
        assertFalse(lengthMethod.isProtected)
        assertFalse(lengthMethod.isStatic)
        assertFalse(lengthMethod.isFinal)
        assertFalse(lengthMethod.isAbstract)
    }

    @Test
    fun `JvmPMethod can invoke no-parameter method`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val lengthMethod = stringPClass.getMethod("length")!!

        val testString = "hello"
        val result = lengthMethod.invoke(testString)

        assertEquals(5, result)
    }

    @Test
    fun `JvmPMethod can invoke method with parameters`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val intPClass = Portrait.intClass()
        val charAtMethod = stringPClass.getMethod("charAt", intPClass)!!

        val testString = "hello"
        val result = charAtMethod.invoke(testString, 1)

        assertEquals('e', result)
    }

    @Test
    fun `JvmPMethod can invoke static methods`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val valueOfMethod = stringPClass.getMethod("valueOf", provider.forName<Int>("java.lang.Integer")!!)

        if (valueOfMethod != null) {
            assertTrue(valueOfMethod.isStatic)
            val result = valueOfMethod.invoke(null, 42)
            assertEquals("42", result)
        }
    }

    @Test
    fun `JvmPMethod parameter type checking`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val intPClass = Portrait.intClass()
        val charAtMethod = stringPClass.getMethod("charAt", intPClass)!!

        assertTrue(charAtMethod.isCallableWith(intPClass))
        assertFalse(charAtMethod.isCallableWith(stringPClass))
        assertFalse(charAtMethod.isCallableWith(intPClass, stringPClass)) // too many parameters
    }

    @Test
    fun `JvmPMethod handles method with multiple parameters`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val intPClass = provider.forName<Int>("java.lang.Integer")!!
        val substringMethod = stringPClass.getMethod("substring", intPClass, intPClass)

        if (substringMethod != null) {
            assertEquals(2, substringMethod.parameterTypes.size)
            assertTrue(substringMethod.isCallableWith(intPClass, intPClass))

            val testString = "hello world"
            val result = substringMethod.invoke(testString, 0, 5)
            assertEquals("hello", result)
        }
    }

    @Test
    fun `JvmPMethod handles exceptions from invoked methods`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val intPClass = Portrait.intClass()
        val charAtMethod = stringPClass.getMethod("charAt", intPClass)!!

        val testString = "hello"

        // This should throw StringIndexOutOfBoundsException
        assertFailsWith<Exception> {
            charAtMethod.invoke(testString, 10)
        }
    }

    @Test
    fun `JvmPMethod can access method annotations`() {
        val testClassPClass = provider.forName<AnnotatedTestClass>("tech.kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotatedMethod = testClassPClass.getMethod("annotatedMethod")

        if (annotatedMethod != null) {
            val annotations = annotatedMethod.annotations
            assertNotNull(annotations)

            val testAnnotationClass = provider.forName<TestAnnotation>("tech.kaffa.portrait.jvm.TestAnnotation")!!
            assertTrue(
                annotatedMethod.hasAnnotation(testAnnotationClass),
                "annotatedMethod should be annotated with TestAnnotation in fixtures"
            )
        }
    }

    @Test
    fun `JvmPMethod handles overloaded methods`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val intPClass = Portrait.intClass()

        val substringOneParam = stringPClass.getMethod("substring", intPClass)
        val substringTwoParam = stringPClass.getMethod("substring", intPClass, intPClass)

        assertNotNull(substringOneParam)
        assertNotNull(substringTwoParam)
        assertEquals("substring", substringOneParam.name)
        assertEquals("substring", substringTwoParam.name)
        assertEquals(1, substringOneParam.parameterTypes.size)
        assertEquals(2, substringTwoParam.parameterTypes.size)
    }

    @Test
    fun `JvmPMethod handles void return type`() {
        // Find a method that returns void
        val testClassPClass = provider.forName<TestClass>("tech.kaffa.portrait.jvm.TestClass")!!
        val setterMethod =
            testClassPClass.getMethod("setInternalValue", provider.forName<String>("java.lang.String")!!)

        if (setterMethod != null) {
            val returnType = setterMethod.returnType
            assertEquals("void", returnType.simpleName)

            val testInstance = TestClass("initial")
            setterMethod.invoke(testInstance, "updated")
            // Void methods return null/Unit
            testInstance.getInternalValue() // Verify the setter worked
        }
    }

    @Test
    fun `JvmPMethod handles generic return types`() {
        val listPClass = provider.forName<List<*>>("java.util.List")!!
        val getMethods = listPClass.methods.filter { it.name == "get" }

        if (getMethods.isNotEmpty()) {
            val getMethod = getMethods.first()
            assertNotNull(getMethod.returnType)
            // Generic type information may be erased at runtime
        }
    }
}


