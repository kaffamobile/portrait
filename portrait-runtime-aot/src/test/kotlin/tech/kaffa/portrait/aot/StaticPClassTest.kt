package tech.kaffa.portrait.aot

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.PConstructorEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry
import tech.kaffa.portrait.aot.meta.serde.MetadataSerializer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaticPClassTest {

    private fun createTestClassEntry(): PClassEntry {
        return PClassEntry(
            simpleName = "TestClass",
            qualifiedName = "com.example.TestClass",
            isAbstract = false,
            isSealed = false,
            isData = true,
            isCompanion = false,
            isObject = false,
            isEnum = false,
            javaClassName = "com.example.TestClass",
            superclassName = "java.lang.Object",
            interfaceNames = listOf("java.io.Serializable"),
            annotations = listOf(
                PAnnotationEntry(
                    annotationClassName = "tech.kaffa.portrait.aot.TestAnnotation",
                    simpleName = "TestAnnotation",
                    qualifiedName = "tech.kaffa.portrait.aot.TestAnnotation",
                    properties = mapOf("value" to "test")
                )
            ),
            constructors = listOf(
                PConstructorEntry(
                    declaringClassName = "com.example.TestClass",
                    parameterTypeNames = emptyList(),
                    annotations = emptyList()
                )
            ),
            declaredMethods = listOf(
                PMethodEntry(
                    name = "testMethod",
                    parameterTypeNames = listOf("java.lang.String"),
                    returnTypeName = "int",
                    declaringClassName = "com.example.TestClass",
                    isStatic = false,
                    isFinal = false,
                    isAbstract = false,
                    annotations = emptyList(),
                    parameterAnnotations = emptyList()
                )
            ),
            declaredFields = listOf(
                PFieldEntry(
                    name = "testField",
                    typeName = "java.lang.String",
                    declaringClassName = "com.example.TestClass",
                    isStatic = false,
                    isFinal = true,
                    annotations = emptyList()
                )
            ),
            proxyMethods = listOf(
                PMethodEntry(
                    name = "proxyTestMethod",
                    parameterTypeNames = listOf("java.lang.String"),
                    returnTypeName = "int",
                    declaringClassName = "com.example.TestClass\$Proxy",
                    isStatic = false,
                    isFinal = false,
                    isAbstract = false,
                    annotations = emptyList(),
                    parameterAnnotations = emptyList()
                )
            )
        )
    }

    private fun metadataFromTestClassEntry(): String {
        return MetadataSerializer().serialize(createTestClassEntry())
    }

    private fun createBoxedIntegerEntry(): PClassEntry {
        return PClassEntry(
            simpleName = "Integer",
            qualifiedName = "java.lang.Integer",
            isAbstract = false,
            isSealed = false,
            isData = false,
            isCompanion = false,
            isObject = false,
            isEnum = false,
            javaClassName = "java.lang.Integer",
            superclassName = "java.lang.Number",
            interfaceNames = listOf("java.lang.Comparable", "java.io.Serializable"),
            annotations = emptyList(),
            constructors = emptyList(),
            declaredMethods = emptyList(),
            declaredFields = emptyList(),
            proxyMethods = emptyList()
        )
    }

    private fun metadataFromBoxedIntegerEntry(): String {
        return MetadataSerializer().serialize(createBoxedIntegerEntry())
    }

    private fun createTestSingletonEntry(): PClassEntry {
        return PClassEntry(
            simpleName = "TestSingleton",
            qualifiedName = "com.example.TestSingleton",
            isAbstract = false,
            isSealed = false,
            isData = false,
            isCompanion = false,
            isObject = true,
            isEnum = false,
            javaClassName = "com.example.TestSingleton",
            superclassName = "java.lang.Object",
            interfaceNames = emptyList(),
            annotations = emptyList(),
            constructors = emptyList(),
            declaredMethods = emptyList(),
            declaredFields = emptyList(),
            proxyMethods = emptyList()
        )
    }

    private fun metadataFromTestSingletonEntry(): String {
        return MetadataSerializer().serialize(createTestSingletonEntry())
    }

    @Test
    fun `StaticPClass basic properties from metadata`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        assertEquals("TestClass", staticPClass.simpleName)
        assertEquals("com.example.TestClass", staticPClass.qualifiedName)
        assertFalse(staticPClass.isAbstract)
        assertFalse(staticPClass.isSealed)
        assertTrue(staticPClass.isData)
        assertFalse(staticPClass.isCompanion)
        assertNull(staticPClass.objectInstance)
    }

    @Test
    fun `StaticPClass can find declared methods`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val method = staticPClass.getMethod("testMethod")
        assertNotNull(method, "Should find testMethod")
        assertEquals("testMethod", method.name)

        val nonExistentMethod = staticPClass.getMethod("nonExistent")
        assertNull(nonExistentMethod, "Should return null for non-existent method")
    }

    @Test
    fun `StaticPClass can find methods with parameters`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val stringPClass = mockk<PClass<String>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()
        every { stringPClass.qualifiedName } returns "java.lang.String"

        val staticPClass = StaticPClass(mockPortrait)

        val method = staticPClass.getMethod("testMethod", stringPClass)
        assertNotNull(method, "Should find testMethod with String parameter")
        assertEquals(1, method.parameterTypes.size)
    }

    @Test
    fun `StaticPClass returns all declared methods`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val methods = staticPClass.methods
        assertEquals(1, methods.size)
        assertEquals("testMethod", methods[0].name)
    }

    @Test
    fun `StaticPClass can find declared fields`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val field = staticPClass.getField("testField")
        assertNotNull(field, "Should find testField")
        assertEquals("testField", field.name)

        val nonExistentField = staticPClass.getField("nonExistent")
        assertNull(nonExistentField, "Should return null for non-existent field")
    }

    @Test
    fun `StaticPClass returns all declared fields`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val fields = staticPClass.fields
        assertEquals(1, fields.size)
        assertEquals("testField", fields[0].name)
    }

    @Test
    fun `StaticPClass exposes proxy methods through indexer`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val proxyMethod = staticPClass.method(0)
        assertEquals("proxyTestMethod", proxyMethod.name)
    }

    @Test
    fun `StaticPClass can find declared constructors`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val constructor = staticPClass.getConstructor()
        assertNotNull(constructor, "Should find default constructor")
        assertEquals(0, constructor.parameterTypes.size)
    }

    @Test
    fun `StaticPClass returns all declared constructors`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val constructors = staticPClass.constructors
        assertEquals(1, constructors.size)
    }

    @Test
    fun `StaticPClass inheritance relationships`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        // Superclass should be loaded via Portrait.forName
        // This will likely be null in unit tests without full Portrait setup

        val interfaces = staticPClass.interfaces
        assertEquals(1, interfaces.size)
        // Interface resolution also depends on Portrait.forName
    }


    @Test
    fun `StaticPClass treats boxed primitives as assignable from primitives`() {
        val mockPortrait = mockk<StaticPortrait<Int>>()

        every { mockPortrait.getClassName() } returns "java.lang.Integer"
        every { mockPortrait.getMetadata() } returns metadataFromBoxedIntegerEntry()

        val staticPClass = StaticPClass(mockPortrait)
        val intPrimitive = Portrait.intClass()

        assertTrue(staticPClass.isAssignableFrom(intPrimitive))
    }

    @Test
    fun `StaticPClass instance creation delegates to portrait`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val testInstance = TestClass("test")

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()
        every { mockPortrait.invokeConstructor(0, emptyArray()) } returns testInstance

        val staticPClass = StaticPClass(mockPortrait)

        val instance = staticPClass.createInstance()
        assertEquals(testInstance, instance)
    }

    @Test
    fun `StaticPClass object instance handling`() {
        val mockPortrait = mockk<StaticPortrait<TestSingleton>>()

        every { mockPortrait.getClassName() } returns "com.example.TestSingleton"
        every { mockPortrait.getMetadata() } returns metadataFromTestSingletonEntry()
        every { mockPortrait.getObjectInstance() } returns TestSingleton

        val staticPClass = StaticPClass(mockPortrait)

        assertNotNull(staticPClass.objectInstance)
        assertEquals(TestSingleton, staticPClass.objectInstance)
    }

    @Test
    fun `StaticPClass handles annotations`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        // Annotation setup for testing annotations

        every { mockPortrait.getClassName() } returns "com.example.TestClass"
        every { mockPortrait.getMetadata() } returns metadataFromTestClassEntry()

        val staticPClass = StaticPClass(mockPortrait)

        val annotations = staticPClass.annotations
        assertEquals(1, annotations.size)
        assertEquals("TestAnnotation", annotations[0].annotationClass.simpleName)
    }
}

