package tech.kaffa.portrait

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PClassExtensionsTest {

    @Test
    fun `boxedOrNull wraps primitives`() {
        val primitive = Portrait.intClass()

        val boxed = primitive.boxedOrNull()

        assertNotNull(boxed)
        assertEquals("java.lang.Integer", boxed.qualifiedName)
        assertTrue(boxed === boxed.boxed())
    }

    @Test
    fun `boxed returns receiver when no boxed form exists`() {
        val stringDescriptor = Portrait.forNameOrUnresolved("java.lang.String")

        val result = stringDescriptor.boxed()

        assertSame(stringDescriptor, result)
    }

    @Test
    fun `unboxedOrNull unwraps boxed primitives`() {
        val wrapper = Portrait.forNameOrUnresolved("java.lang.Integer")

        val primitive = wrapper.unboxedOrNull()

        assertNotNull(primitive)
        assertEquals("int", primitive.qualifiedName)
        assertTrue(primitive === primitive.unboxed())
    }

    @Test
    fun `unboxed returns receiver when no primitive form exists`() {
        val stringDescriptor = Portrait.forNameOrUnresolved("java.lang.String")

        val result = stringDescriptor.unboxed()

        assertSame(stringDescriptor, result)
    }
}
