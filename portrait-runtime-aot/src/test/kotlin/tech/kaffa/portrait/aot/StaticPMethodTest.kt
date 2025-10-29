package tech.kaffa.portrait.aot

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry
import tech.kaffa.portrait.aot.meta.PClassTypeEntry
import tech.kaffa.portrait.aot.meta.PParameterizedTypeEntry
import tech.kaffa.portrait.typeName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticPMethodTest {

    private fun createTestMethodEntry(): PMethodEntry {
        return PMethodEntry(
            name = "testMethod",
            parameterTypeNames = listOf("java.lang.String", "int"),
            returnTypeName = "java.lang.Object",
            genericReturnType = PClassTypeEntry("java.lang.Object"),
            declaringClassName = "com.example.TestClass",
            isStatic = false,
            isFinal = true,
            isAbstract = false,
            annotations = listOf(
                PAnnotationEntry(
                    annotationClassName = "com.example.TestAnnotation",
                    simpleName = "TestAnnotation",
                    qualifiedName = "com.example.TestAnnotation",
                    properties = mapOf("value" to "method-level")
                )
            ),
            parameterAnnotations = emptyList()
        )
    }

    @Test
    fun `StaticPMethod basic properties from metadata`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val methodEntry = createTestMethodEntry()

        val staticPMethod = StaticPMethod(methodEntry, 0, mockPortrait)

        assertEquals("testMethod", staticPMethod.name)
        assertEquals(2, staticPMethod.parameterTypes.size)
        assertFalse(staticPMethod.isStatic)
        assertTrue(staticPMethod.isFinal)
        assertFalse(staticPMethod.isAbstract)
    }

    @Test
    fun `StaticPMethod parameter types resolution`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val methodEntry = createTestMethodEntry()

        val staticPMethod = StaticPMethod(methodEntry, 0, mockPortrait)

        // Parameter types should be resolved via Portrait.forName
        assertEquals(2, staticPMethod.parameterTypes.size)
        // In real scenario, these would be resolved PClass instances
    }

    @Test
    fun `StaticPMethod return type resolution`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val methodEntry = createTestMethodEntry()

        val staticPMethod = StaticPMethod(methodEntry, 0, mockPortrait)

        // Return type should be resolved via Portrait.forName
        assertFalse(staticPMethod.returnType.toString().isEmpty())
        assertEquals("java.lang.Object", staticPMethod.genericReturnType.typeName())
    }

    @Test
    fun `StaticPMethod parameter matching`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val methodEntry = createTestMethodEntry()

        val staticPMethod = StaticPMethod(methodEntry, 0, mockPortrait)

        // Get the actual resolved parameter types from the method
        val stringType = staticPMethod.parameterTypes[0]
        val intType = staticPMethod.parameterTypes[1]

        // Should match exact parameter types
        assertTrue(staticPMethod.isCallableWith(stringType, intType))
        assertFalse(staticPMethod.isCallableWith(stringType)) // too few parameters
        assertFalse(staticPMethod.isCallableWith(stringType, intType, stringType)) // too many
    }

    @Test
    fun `StaticPMethod invocation delegates to portrait`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val methodEntry = createTestMethodEntry()
        val testInstance = TestClass("test")
        val expectedResult = "method_result"

        every { mockPortrait.invokeMethod(1, testInstance, arrayOf("param1", 42)) } returns expectedResult

        val staticPMethod = StaticPMethod(methodEntry, 1, mockPortrait)

        val result = staticPMethod.invoke(testInstance, "param1", 42)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `StaticPMethod static method invocation`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val staticMethodEntry = createTestMethodEntry().copy(isStatic = true)
        val expectedResult = "static_result"

        every { mockPortrait.invokeMethod(1, null, arrayOf("param1", 42)) } returns expectedResult

        val staticPMethod = StaticPMethod(staticMethodEntry, 1, mockPortrait)

        assertTrue(staticPMethod.isStatic)
        val result = staticPMethod.invoke(null, "param1", 42)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `StaticPMethod handles void return type`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val voidMethodEntry = createTestMethodEntry().copy(
            returnTypeName = "void",
            genericReturnType = PClassTypeEntry("void")
        )
        val testInstance = TestClass("test")

        every { mockPortrait.invokeMethod(1, testInstance, arrayOf("param1", 42)) } returns null

        val staticPMethod = StaticPMethod(voidMethodEntry, 1, mockPortrait)

        val result = staticPMethod.invoke(testInstance, "param1", 42)
        assertEquals(null, result) // void methods return null
        assertEquals("void", staticPMethod.genericReturnType.typeName())
    }

    @Test
    fun `StaticPMethod exposes parameterized generic return type`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val parameterizedEntry = createTestMethodEntry().copy(
            returnTypeName = "java.util.List",
            genericReturnType = PParameterizedTypeEntry(
                rawTypeName = "java.util.List",
                ownerType = null,
                arguments = listOf(PClassTypeEntry("java.lang.String"))
            )
        )

        val staticPMethod = StaticPMethod(parameterizedEntry, 0, mockPortrait)

        assertEquals("java.util.List<java.lang.String>", staticPMethod.genericReturnType.typeName())
    }

    @Test
    fun `StaticPMethod annotation handling`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val methodEntry = createTestMethodEntry()

        val staticPMethod = StaticPMethod(methodEntry, 0, mockPortrait)

        val annotations = staticPMethod.annotations
        assertEquals(1, annotations.size)
        assertEquals("TestAnnotation", annotations[0].annotationClass.simpleName)

        // Test annotation resolution methods
        val actualAnnotationClass = annotations[0].annotationClass
        assertTrue(staticPMethod.hasAnnotation(actualAnnotationClass))
        assertNotNull(staticPMethod.getAnnotation(actualAnnotationClass))
    }

    @Test
    fun `StaticPMethod parameter annotations`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val methodWithParamAnnotations = createTestMethodEntry().copy(
            parameterAnnotations = listOf(
                listOf(
                    PAnnotationEntry(
                        annotationClassName = "com.example.ParamAnnotation",
                        simpleName = "ParamAnnotation",
                        qualifiedName = "com.example.ParamAnnotation",
                        properties = mapOf("value" to "param1")
                    )
                ),
                emptyList() // second parameter has no annotations
            )
        )

        val staticPMethod = StaticPMethod(methodWithParamAnnotations, -1, mockPortrait)

        assertEquals(2, staticPMethod.parameterTypes.size)
        // Parameter annotations would be accessible through the parameter types
        // in a full implementation
    }

    @Test
    fun `StaticPMethod primitive parameter types`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val primitiveMethodEntry = createTestMethodEntry().copy(
            parameterTypeNames = listOf("int", "boolean", "double")
        )

        val staticPMethod = StaticPMethod(primitiveMethodEntry, 0, mockPortrait)

        assertEquals(3, staticPMethod.parameterTypes.size)
        // Primitive types should be resolved to their wrapper classes or primitive PClass representations
    }

    @Test
    fun `StaticPMethod method overloading support`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        val method1 = createTestMethodEntry().copy(
            parameterTypeNames = listOf("java.lang.String")
        )
        val method2 = createTestMethodEntry().copy(
            parameterTypeNames = listOf("java.lang.String", "int")
        )

        val staticPMethod1 = StaticPMethod(method1, -1, mockPortrait)
        val staticPMethod2 = StaticPMethod(method2, -1, mockPortrait)

        assertEquals("testMethod", staticPMethod1.name)
        assertEquals("testMethod", staticPMethod2.name)
        assertEquals(1, staticPMethod1.parameterTypes.size)
        assertEquals(2, staticPMethod2.parameterTypes.size)
    }
}
