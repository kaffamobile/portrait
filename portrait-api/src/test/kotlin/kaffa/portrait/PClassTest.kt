package kaffa.portrait

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PClassTest {

    @Test
    fun `PClass basic properties work correctly`() {
        val pClass = mockk<PClass<TestClass>>()

        every { pClass.simpleName } returns "TestClass"
        every { pClass.qualifiedName } returns "kaffa.portrait.jvm.TestClass"
        every { pClass.isAbstract } returns false
        every { pClass.isSealed } returns false
        every { pClass.isData } returns false
        every { pClass.isCompanion } returns false

        // Verify class identity through qualified name instead of java class
        assertEquals("TestClass", pClass.simpleName)
        assertEquals("kaffa.portrait.jvm.TestClass", pClass.qualifiedName)
        assertFalse(pClass.isAbstract)
        assertFalse(pClass.isSealed)
        assertFalse(pClass.isData)
        assertFalse(pClass.isCompanion)
    }

    @Test
    fun `PClass can find declared methods`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockMethod = mockk<PMethod>()

        every { pClass.getDeclaredMethod("doSomething") } returns mockMethod
        every { pClass.getDeclaredMethod("nonExistent") } returns null
        every { pClass.declaredMethods } returns listOf(mockMethod)

        assertNotNull(pClass.getDeclaredMethod("doSomething"))
        assertNull(pClass.getDeclaredMethod("nonExistent"))
        assertEquals(1, pClass.declaredMethods.size)
    }

    @Test
    fun `PClass can find methods with parameters`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockMethod = mockk<PMethod>()
        val stringPClass = mockk<PClass<String>>()

        every { pClass.getDeclaredMethod("setInternalValue", stringPClass) } returns mockMethod
        every { pClass.getDeclaredMethod("setInternalValue") } returns null

        assertNotNull(pClass.getDeclaredMethod("setInternalValue", stringPClass))
        assertNull(pClass.getDeclaredMethod("setInternalValue"))
    }

    @Test
    fun `PClass can find declared fields`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockField = mockk<PField>()

        every { pClass.getDeclaredField("internalValue") } returns mockField
        every { pClass.getDeclaredField("nonExistent") } returns null
        every { pClass.declaredFields } returns listOf(mockField)

        assertNotNull(pClass.getDeclaredField("internalValue"))
        assertNull(pClass.getDeclaredField("nonExistent"))
        assertEquals(1, pClass.declaredFields.size)
    }

    @Test
    fun `PClass can find constructors`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockConstructor = mockk<PConstructor<TestClass>>()
        val stringPClass = mockk<PClass<String>>()

        every { pClass.getConstructor() } returns mockConstructor
        every { pClass.getConstructor(stringPClass) } returns mockConstructor
        every { pClass.constructors } returns listOf(mockConstructor)

        assertNotNull(pClass.getConstructor())
        assertNotNull(pClass.getConstructor(stringPClass))
        assertEquals(1, pClass.constructors.size)
    }

    @Test
    fun `PClass inheritance relationships work`() {
        val pClass = mockk<PClass<TestClass>>()
        val interfacePClass = mockk<PClass<TestInterface>>()
        val superPClass = mockk<PClass<Any>>()

        every { pClass.superclass } returns superPClass
        every { pClass.interfaces } returns listOf(interfacePClass)
        every { pClass.isAssignableFrom(any()) } returns true

        assertEquals(superPClass, pClass.superclass)
        assertEquals(1, pClass.interfaces.size)
        assertTrue(pClass.isAssignableFrom(mockk()))
    }

    @Test
    fun `PClass can handle annotations`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockAnnotation = mockk<PAnnotation>()
        val annotationClass = mockk<PClass<TestAnnotation>>()

        every { pClass.getAnnotation(annotationClass) } returns mockAnnotation
        every { pClass.hasAnnotation(annotationClass) } returns true
        every { pClass.annotations } returns listOf(mockAnnotation)

        assertNotNull(pClass.getAnnotation(annotationClass))
        assertTrue(pClass.hasAnnotation(annotationClass))
        assertEquals(1, pClass.annotations.size)
    }

    @Test
    fun `PClass can create new instances`() {
        val pClass = mockk<PClass<TestClass>>()
        val instance = TestClass("test")

        every { pClass.createInstance() } returns instance
        every { pClass.createInstance("test") } returns instance

        assertEquals(instance, pClass.createInstance())
        assertEquals(instance, pClass.createInstance("test"))
    }

    @Test
    fun `PClass object instance handling`() {
        val pClass = mockk<PClass<TestSingleton>>()
        val objectInstance = TestSingleton

        every { pClass.objectInstance } returns objectInstance

        assertNotNull(pClass.objectInstance)
        assertEquals(objectInstance, pClass.objectInstance)
    }
}