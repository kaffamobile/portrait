package tech.kaffa.portrait

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import tech.kaffa.portrait.proxy.ProxyHandler
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the abstract base classes to verify their contracts and structure.
 */
class AbstractClassesTest {

    @Test
    fun `PMethod abstract class has correct structure`() {
        val pMethod = mockk<PMethod>()

        every { pMethod.name } returns "testMethod"
        every { pMethod.parameterTypes } returns emptyList()
        every { pMethod.returnType } returns mockk()
        every { pMethod.declaringClass } returns mockk()
        every { pMethod.annotations } returns emptyList()
        every { pMethod.isPublic } returns true
        every { pMethod.isPrivate } returns false
        every { pMethod.isProtected } returns false
        every { pMethod.isStatic } returns false
        every { pMethod.isFinal } returns false
        every { pMethod.isAbstract } returns false

        assertEquals("testMethod", pMethod.name)
        assertNotNull(pMethod.parameterTypes)
        assertNotNull(pMethod.returnType)
        assertNotNull(pMethod.declaringClass)
        assertNotNull(pMethod.annotations)
    }

    @Test
    fun `PField abstract class has correct structure`() {
        val pField = mockk<PField>()

        every { pField.name } returns "testField"
        every { pField.type } returns mockk()
        every { pField.declaringClass } returns mockk()
        every { pField.annotations } returns emptyList()
        every { pField.isPublic } returns true
        every { pField.isPrivate } returns false
        every { pField.isProtected } returns false
        every { pField.isStatic } returns false
        every { pField.isFinal } returns false

        assertEquals("testField", pField.name)
        assertNotNull(pField.type)
        assertNotNull(pField.declaringClass)
        assertNotNull(pField.annotations)
    }

    @Test
    fun `PConstructor abstract class has correct structure`() {
        val pConstructor = mockk<PConstructor<Any>>()

        every { pConstructor.parameterTypes } returns emptyList()
        every { pConstructor.declaringClass } returns mockk()
        every { pConstructor.annotations } returns emptyList()
        every { pConstructor.isPublic } returns true
        every { pConstructor.isPrivate } returns false
        every { pConstructor.isProtected } returns false

        assertNotNull(pConstructor.parameterTypes)
        assertNotNull(pConstructor.declaringClass)
        assertNotNull(pConstructor.annotations)
    }

    @Test
    fun `PAnnotation abstract class has correct structure`() {
        val pAnnotation = mockk<PAnnotation>()
        val annotationClass = mockk<PClass<out Annotation>>()

        every { pAnnotation.annotationClass } returns annotationClass
        every { pAnnotation.getValue("value") } returns "test"
        every { pAnnotation.getValue("number") } returns 42

        assertEquals(annotationClass, pAnnotation.annotationClass)
        assertEquals("test", pAnnotation.getValue("value"))
        assertEquals(42, pAnnotation.getValue("number"))
    }

    @Test
    fun `ProxyHandler abstract class has correct structure`() {
        val proxyHandler = mockk<ProxyHandler<Any>>()
        val mockMethod = mockk<PMethod>()
        val result = "test result"

        every { proxyHandler.invoke(any(), mockMethod, any()) } returns result

        assertEquals(result, proxyHandler.invoke(mockk(), mockMethod, emptyArray()))
    }

    @Test
    fun `Abstract classes can be implemented by concrete classes`() {
        // This test verifies that the abstract classes can be extended
        // and all abstract methods are properly defined

        val concreteMethod = mockk<PMethod>()

        every { concreteMethod.name } returns "testMethod"
        every { concreteMethod.parameterTypes } returns emptyList<PClass<*>>()
        every { concreteMethod.parameterCount } returns 0
        every { concreteMethod.returnType } returns mockk<PClass<*>>()
        every { concreteMethod.declaringClass } returns mockk<PClass<*>>()
        every { concreteMethod.annotations } returns emptyList<PAnnotation>()
        every { concreteMethod.isPublic } returns true
        every { concreteMethod.isPrivate } returns false
        every { concreteMethod.isProtected } returns false
        every { concreteMethod.isStatic } returns false
        every { concreteMethod.isFinal } returns false
        every { concreteMethod.isAbstract } returns false
        every { concreteMethod.invoke(any(), *anyVararg()) } returns "result"
        every { concreteMethod.isCallableWith(*anyVararg()) } returns true
        every { concreteMethod.getAnnotation(any()) } returns null
        every { concreteMethod.hasAnnotation(any()) } returns false
        every { concreteMethod.parameterAnnotations } returns emptyList()

        assertEquals("testMethod", concreteMethod.name)
        assertEquals("result", concreteMethod.invoke(null))
    }
}
