package tech.kaffa.portrait.aot

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PConstructorEntry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticPConstructorTest {

    private fun createTestConstructorEntry(): PConstructorEntry {
        return PConstructorEntry(
            declaringClassName = "com.example.TestClass",
            parameterTypeNames = listOf("java.lang.String", "int"),
            annotations = listOf(
                PAnnotationEntry(
                    annotationClassName = "com.example.ConstructorAnnotation",
                    simpleName = "ConstructorAnnotation",
                    qualifiedName = "com.example.ConstructorAnnotation",
                    properties = mapOf("value" to "constructor-level")
                )
            )
        )
    }

    @Test
    fun `StaticPConstructor basic properties from metadata`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val constructorEntry = createTestConstructorEntry()
        val mockDeclaringClass = mockk<PClass<TestClass>>()

        val staticPConstructor = StaticPConstructor(constructorEntry, -1, mockDeclaringClass, mockPortrait)

        assertEquals(2, staticPConstructor.parameterTypes.size)
        assertEquals(mockDeclaringClass, staticPConstructor.declaringClass)
    }

    @Test
    fun `StaticPConstructor parameter types resolution`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val constructorEntry = createTestConstructorEntry()
        val mockDeclaringClass = mockk<PClass<TestClass>>()

        val staticPConstructor = StaticPConstructor(constructorEntry, -1, mockDeclaringClass, mockPortrait)

        // Parameter types should be resolved via Portrait.forName
        assertEquals(2, staticPConstructor.parameterTypes.size)
    }

    @Test
    fun `StaticPConstructor parameter matching`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val constructorEntry = createTestConstructorEntry()
        val mockDeclaringClass = mockk<PClass<TestClass>>()

        val staticPConstructor = StaticPConstructor(constructorEntry, -1, mockDeclaringClass, mockPortrait)

        // Get the actual resolved parameter types from the constructor
        val stringType = staticPConstructor.parameterTypes[0]
        val intType = staticPConstructor.parameterTypes[1]

        // Should match exact parameter types
        assertTrue(staticPConstructor.isCallableWith(stringType, intType))
        assertFalse(staticPConstructor.isCallableWith(stringType)) // too few parameters
        assertFalse(staticPConstructor.isCallableWith(stringType, intType, stringType)) // too many
    }

    @Test
    fun `StaticPConstructor instance creation delegates to portrait`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val constructorEntry = createTestConstructorEntry()
        val mockDeclaringClass = mockk<PClass<TestClass>>()
        val expectedInstance = TestClass("constructed")

        every { mockPortrait.invokeConstructor(0, arrayOf("param1", 42)) } returns expectedInstance

        val staticPConstructor = StaticPConstructor(constructorEntry, 0, mockDeclaringClass, mockPortrait)

        val instance = staticPConstructor.call("param1", 42)
        assertEquals(expectedInstance, instance)
    }

    @Test
    fun `StaticPConstructor default constructor`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val defaultConstructorEntry = createTestConstructorEntry().copy(
            parameterTypeNames = emptyList()
        )
        val mockDeclaringClass = mockk<PClass<TestClass>>()
        val expectedInstance = TestClass()

        every { mockPortrait.invokeConstructor(0, emptyArray()) } returns expectedInstance

        val staticPConstructor = StaticPConstructor(defaultConstructorEntry, 0, mockDeclaringClass, mockPortrait)

        assertEquals(0, staticPConstructor.parameterTypes.size)
        assertTrue(staticPConstructor.isCallableWith())

        val instance = staticPConstructor.call()
        assertEquals(expectedInstance, instance)
    }

    @Test
    fun `StaticPConstructor handles primitive parameter types`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val primitiveConstructorEntry = createTestConstructorEntry().copy(
            parameterTypeNames = listOf("int", "boolean", "double")
        )
        val mockDeclaringClass = mockk<PClass<TestClass>>()
        val expectedInstance = TestClass("primitive")

        every { mockPortrait.invokeConstructor(0, arrayOf(42, true, 3.14)) } returns expectedInstance

        val staticPConstructor = StaticPConstructor(primitiveConstructorEntry, 0, mockDeclaringClass, mockPortrait)

        assertEquals(3, staticPConstructor.parameterTypes.size)

        val instance = staticPConstructor.call(42, true, 3.14)
        assertEquals(expectedInstance, instance)
    }

    @Test
    fun `StaticPConstructor handles null parameter values`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val constructorEntry = createTestConstructorEntry()
        val mockDeclaringClass = mockk<PClass<TestClass>>()
        val expectedInstance = TestClass("null_param")

        every { mockPortrait.invokeConstructor(0, arrayOf(null, 42)) } returns expectedInstance

        val staticPConstructor = StaticPConstructor(constructorEntry, 0, mockDeclaringClass, mockPortrait)

        val instance = staticPConstructor.call(null, 42)
        assertEquals(expectedInstance, instance)
    }

    @Test
    fun `StaticPConstructor annotation handling`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val constructorEntry = createTestConstructorEntry()
        val mockDeclaringClass = mockk<PClass<TestClass>>()

        val staticPConstructor = StaticPConstructor(constructorEntry, -1, mockDeclaringClass, mockPortrait)

        val annotations = staticPConstructor.annotations
        assertEquals(1, annotations.size)
        assertEquals("ConstructorAnnotation", annotations[0].annotationClass.simpleName)

        // Test annotation resolution methods
        val actualAnnotationClass = annotations[0].annotationClass
        assertTrue(staticPConstructor.hasAnnotation(actualAnnotationClass))
        assertNotNull(staticPConstructor.getAnnotation(actualAnnotationClass))
    }

    @Test
    fun `StaticPConstructor constructor overloading`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val mockDeclaringClass = mockk<PClass<TestClass>>()

        val constructor1 = createTestConstructorEntry().copy(
            parameterTypeNames = emptyList()
        )
        val constructor2 = createTestConstructorEntry().copy(
            parameterTypeNames = listOf("java.lang.String")
        )
        val constructor3 = createTestConstructorEntry().copy(
            parameterTypeNames = listOf("java.lang.String", "int")
        )

        val staticPConstructor1 = StaticPConstructor(constructor1, 0, mockDeclaringClass, mockPortrait)
        val staticPConstructor2 = StaticPConstructor(constructor2, 0, mockDeclaringClass, mockPortrait)
        val staticPConstructor3 = StaticPConstructor(constructor3, 0, mockDeclaringClass, mockPortrait)

        assertEquals(0, staticPConstructor1.parameterTypes.size)
        assertEquals(1, staticPConstructor2.parameterTypes.size)
        assertEquals(2, staticPConstructor3.parameterTypes.size)

        // Each should be callable with its specific parameter signature
        assertTrue(staticPConstructor1.isCallableWith())
        assertTrue(staticPConstructor2.isCallableWith(staticPConstructor2.parameterTypes[0]))
        assertTrue(
            staticPConstructor3.isCallableWith(
                staticPConstructor3.parameterTypes[0],
                staticPConstructor3.parameterTypes[1]
            )
        )
    }

    @Test
    fun `StaticPConstructor handles array parameter types`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val arrayConstructorEntry = createTestConstructorEntry().copy(
            parameterTypeNames = listOf("[Ljava.lang.String;", "[I")
        )
        val mockDeclaringClass = mockk<PClass<TestClass>>()
        val expectedInstance = TestClass("array_constructor")
        val stringArray = arrayOf("one", "two")
        val intArray = intArrayOf(1, 2, 3)

        every { mockPortrait.invokeConstructor(0, arrayOf(stringArray, intArray)) } returns expectedInstance

        val staticPConstructor = StaticPConstructor(arrayConstructorEntry, 0, mockDeclaringClass, mockPortrait)

        assertEquals(2, staticPConstructor.parameterTypes.size)

        val instance = staticPConstructor.call(stringArray, intArray)
        assertEquals(expectedInstance, instance)
    }
}
