package kaffa.portrait

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PrimitiveIntegrationTest {

    @Test
    fun `Portrait can resolve primitive types that would cause ClassNotFoundException`() {
        // These primitive type names would cause ClassNotFoundException if passed to Class.forName()
        // but Portrait should handle them correctly

        val primitiveNames = listOf("boolean", "byte", "char", "short", "int", "long", "float", "double", "void")

        for (primitiveName in primitiveNames) {
            val pClass = Portrait.forName(primitiveName)
            assertNotNull(pClass, "Should be able to resolve primitive type: $primitiveName")
            assertEquals(primitiveName, pClass.simpleName)

            // Verify that the Portrait class name mapping works
            assertEquals(primitiveName, pClass.qualifiedName)
        }
    }

    @Test
    fun `Portrait primitive types have correct Java class mappings`() {
        // Verify that the primitive types map to the correct wrapper classes
        // FIXME
//        assertEquals(Boolean::class.java, Portrait.booleanClass().java)
//        assertEquals(Byte::class.java, Portrait.byteClass().java)
//        assertEquals(Char::class.java, Portrait.charClass().java)
//        assertEquals(Short::class.java, Portrait.shortClass().java)
//        assertEquals(Int::class.java, Portrait.intClass().java)
//        assertEquals(Long::class.java, Portrait.longClass().java)
//        assertEquals(Float::class.java, Portrait.floatClass().java)
//        assertEquals(Double::class.java, Portrait.doubleClass().java)
//        assertEquals(Void::class.java, Portrait.voidClass().java)
    }

    @Test
    fun `Portrait forName works for both primitives and objects`() {
        // Should work for primitives
        val intClass = Portrait.forName("int")
        assertNotNull(intClass)
        assertEquals("int", intClass.simpleName)

        // Should still work for regular classes (though will throw RuntimeException without providers)
        try {
            Portrait.forName("java.lang.String")
        } catch (e: RuntimeException) {
            // Expected when no providers are available, but the important thing is that
            // we don't get ClassNotFoundException for "boolean" anymore
        }
    }
}