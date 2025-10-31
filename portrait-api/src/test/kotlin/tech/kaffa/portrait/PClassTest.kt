package tech.kaffa.portrait

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import tech.kaffa.portrait.proxy.ProxyHandler
import tech.kaffa.portrait.internal.PrimitivePClass
import tech.kaffa.portrait.internal.UnresolvedPClass

class PClassTest {

    @Test
    fun `PClass basic properties work correctly`() {
        val pClass = mockk<PClass<TestClass>>()

        every { pClass.simpleName } returns "TestClass"
        every { pClass.qualifiedName } returns "tech.kaffa.portrait.jvm.TestClass"
        every { pClass.isAbstract } returns false
        every { pClass.isSealed } returns false
        every { pClass.isData } returns false
        every { pClass.isCompanion } returns false

        // Verify class identity through qualified name instead of java class
        assertEquals("TestClass", pClass.simpleName)
        assertEquals("tech.kaffa.portrait.jvm.TestClass", pClass.qualifiedName)
        assertFalse(pClass.isAbstract)
        assertFalse(pClass.isSealed)
        assertFalse(pClass.isData)
        assertFalse(pClass.isCompanion)
    }

    @Test
    fun `PClass can find declared methods`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockMethod = mockk<PMethod>()

        every { pClass.getMethod("doSomething") } returns mockMethod
        every { pClass.getMethod("nonExistent") } returns null
        every { pClass.methods } returns listOf(mockMethod)

        assertNotNull(pClass.getMethod("doSomething"))
        assertNull(pClass.getMethod("nonExistent"))
        assertEquals(1, pClass.methods.size)
    }

    @Test
    fun `PClass can find methods with parameters`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockMethod = mockk<PMethod>()
        val stringPClass = mockk<PClass<String>>()

        every { pClass.getMethod("setInternalValue", stringPClass) } returns mockMethod
        every { pClass.getMethod("setInternalValue") } returns null

        assertNotNull(pClass.getMethod("setInternalValue", stringPClass))
        assertNull(pClass.getMethod("setInternalValue"))
    }

    @Test
    fun `PClass can find declared fields`() {
        val pClass = mockk<PClass<TestClass>>()
        val mockField = mockk<PField>()

        every { pClass.getField("internalValue") } returns mockField
        every { pClass.getField("nonExistent") } returns null
        every { pClass.fields } returns listOf(mockField)

        assertNotNull(pClass.getField("internalValue"))
        assertNull(pClass.getField("nonExistent"))
        assertEquals(1, pClass.fields.size)
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
        val mockAnnotation = mockk<PAnnotation<TestAnnotation>>()
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

        every { pClass.newInstance() } returns instance
        every { pClass.newInstance("test") } returns instance

        assertEquals(instance, pClass.newInstance())
        assertEquals(instance, pClass.newInstance("test"))
    }

    @Test
    fun `PClass object instance handling`() {
        val pClass = mockk<PClass<TestSingleton>>()
        val objectInstance = TestSingleton

        every { pClass.objectInstance } returns objectInstance

        assertNotNull(pClass.objectInstance)
        assertEquals(objectInstance, pClass.objectInstance)
    }

    @Test
    fun `default getConstructor returns matching signatures`() {
        val zeroArgConstructor = mockk<PConstructor<TestClass>>(relaxed = true)
        val stringParameterType = mockk<PClass<*>>()
        val stringConstructor = mockk<PConstructor<TestClass>>(relaxed = true)

        every { zeroArgConstructor.parameterTypes } returns emptyList()
        every { stringConstructor.parameterTypes } returns listOf(stringParameterType)

        val pClass = TestablePClass(
            qualifiedName = "tech.kaffa.portrait.TestClass",
            constructors = listOf(zeroArgConstructor, stringConstructor)
        )

        assertSame(zeroArgConstructor, pClass.getConstructor())
        assertSame(stringConstructor, pClass.getConstructor(stringParameterType))
        assertNull(pClass.getConstructor(mockk()))
    }

    @Test
    fun `default createInstance delegates to zero argument constructor`() {
        val instance = TestClass("zero-arg")
        val zeroArgConstructor = mockk<PConstructor<TestClass>>()

        every { zeroArgConstructor.parameterTypes } returns emptyList()
        every { zeroArgConstructor.newInstance() } returns instance

        val pClass = TestablePClass(
            qualifiedName = "tech.kaffa.portrait.TestClass",
            constructors = listOf(zeroArgConstructor)
        )

        val created = pClass.newInstance()

        assertSame(instance, created)
        verify(exactly = 1) { zeroArgConstructor.newInstance() }
    }

    @Test
    fun `default createInstance selects constructor compatible with null arguments`() {
        val expected = TestClass("nullable")
        val parameterType = mockk<PClass<*>>()
        every { parameterType.isPrimitive } returns false

        val constructor = mockk<PConstructor<TestClass>>()
        every { constructor.parameterTypes } returns listOf(parameterType)
        every { constructor.newInstance(null) } returns expected

        val pClass = TestablePClass(
            qualifiedName = "tech.kaffa.portrait.TestClass",
            constructors = listOf(constructor)
        )

        val created = pClass.newInstance(null)

        assertSame(expected, created)
        verify(exactly = 1) { constructor.newInstance(null) }
    }

    @Test
    fun `default getMethod matches by name and signature`() {
        val parameterType = mockk<PClass<*>>()
        val matching = mockk<PMethod>()
        val other = mockk<PMethod>()

        every { matching.name } returns "target"
        every { matching.parameterTypes } returns listOf(parameterType)
        every { other.name } returns "target"
        every { other.parameterTypes } returns emptyList()

        val pClass = TestablePClass<TestClass>(
            qualifiedName = "tech.kaffa.portrait.TestClass",
            methods = listOf(matching, other)
        )

        assertSame(matching, pClass.getMethod("target", parameterType))
        assertSame(other, pClass.getMethod("target"))
        assertNull(pClass.getMethod("target", mockk()))
        assertNull(pClass.getMethod("missing"))
    }

    @Test
    fun `default getField finds field by name`() {
        val field = mockk<PField>()
        every { field.name } returns "internalValue"

        val pClass = TestablePClass<TestClass>(
            qualifiedName = "tech.kaffa.portrait.TestClass",
            fields = listOf(field)
        )

        assertSame(field, pClass.getField("internalValue"))
        assertNull(pClass.getField("missing"))
    }

    @Test
    fun `PClass equality compares qualified names across implementations`() {
        val local: PClass<*> = TestablePClass<TestClass>(qualifiedName = "java.lang.String")
        val localDuplicate: PClass<*> = TestablePClass<TestClass>(qualifiedName = "java.lang.String")
        val unresolved: PClass<*> = UnresolvedPClass<TestClass>("java.lang.String")
        val primitive: PClass<*> = PrimitivePClass<Any>("int")
        val primitiveDuplicate: PClass<*> = TestablePClass<TestClass>(qualifiedName = "int")
        val different: PClass<*> = UnresolvedPClass<TestClass>("java.lang.Object")

        assertEquals(local, localDuplicate)
        assertEquals(local, unresolved)
        assertEquals(unresolved, local)
        assertEquals(primitive, primitiveDuplicate)
        assertNotEquals(local, primitive)
        assertNotEquals(local, different)
        assertEquals(local.hashCode(), localDuplicate.hashCode())
        assertEquals(local.hashCode(), unresolved.hashCode())
        assertEquals(primitive.hashCode(), primitiveDuplicate.hashCode())
    }

    @Test
    fun `default annotation helpers consult the annotations list`() {
        val annotationType = mockk<PClass<Annotation>>()
        val annotation = mockk<PAnnotation<Annotation>>()
        every { annotation.annotationClass } returns annotationType

        val pClass = TestablePClass<TestClass>(
            qualifiedName = "tech.kaffa.portrait.TestClass",
            annotations = listOf(annotation)
        )

        assertSame(annotation, pClass.getAnnotation(annotationType))
        assertTrue(pClass.hasAnnotation(annotationType))
        assertNull(pClass.getAnnotation<Annotation>(mockk()))
        assertFalse(pClass.hasAnnotation(mockk()))
    }

    @Test
    fun `default subclass and assignable checks traverse hierarchy`() {
        val parent = TestablePClass<TestClass>(qualifiedName = "tech.kaffa.portrait.Parent")
        val interfaceClass = TestablePClass<TestInterface>(qualifiedName = "tech.kaffa.portrait.Interface")
        val child = TestablePClass<TestClass>(
            qualifiedName = "tech.kaffa.portrait.Child",
            superclass = parent,
            interfaces = listOf(interfaceClass)
        )

        assertTrue(child.isSubclassOf(child))
        assertTrue(child.isSubclassOf(parent))
        assertTrue(child.isSubclassOf(interfaceClass))
        assertTrue(parent.isAssignableFrom(child))
        assertTrue(interfaceClass.isAssignableFrom(child))
        assertFalse(parent.isSubclassOf(child))
    }
}

private class TestablePClass<T : Any>(
    override val qualifiedName: String = "tech.kaffa.portrait.Testable",
    private val isAbstractFlag: Boolean = false,
    private val isSealedFlag: Boolean = false,
    private val isDataFlag: Boolean = false,
    private val isCompanionFlag: Boolean = false,
    private val isEnumFlag: Boolean = false,
    override val constructors: List<PConstructor<T>> = emptyList(),
    override val methods: List<PMethod> = emptyList(),
    override val fields: List<PField> = emptyList(),
    override val annotations: List<PAnnotation<*>> = emptyList(),
    override val superclass: PClass<*>? = null,
    override val interfaces: List<PClass<*>> = emptyList(),
    override val objectInstance: T? = null,
    private val enumConstantValues: Array<T>? = null
) : PClass<T>() {
    override val simpleName: String = qualifiedName.substringAfterLast('.')
    override val isAbstract: Boolean = isAbstractFlag
    override val isSealed: Boolean = isSealedFlag
    override val isData: Boolean = isDataFlag
    override val isCompanion: Boolean = isCompanionFlag
    override val isEnum: Boolean = isEnumFlag
    override val enumConstants: Array<T>? = enumConstantValues

    override fun createProxy(handler: ProxyHandler<T>): T {
        throw UnsupportedOperationException("Proxy creation not supported for test stubs")
    }
}


