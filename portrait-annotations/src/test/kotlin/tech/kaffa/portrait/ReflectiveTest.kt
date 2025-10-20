package tech.kaffa.portrait

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Top-level test classes and interfaces
@Reflective
class TestClass

@Reflective
data class TestDataClass(val id: Int, val name: String)

@Reflective
object TestObject

@Reflective
interface TestInterface

@Reflective
abstract class TestAbstractClass

class ReflectiveTest {

    @Test
    fun `@Reflective annotation can be applied to classes`() {
        val annotation = TestClass::class.java.getAnnotation(Reflective::class.java)
        assertNotNull(annotation, "@Reflective annotation should be present on class")
    }

    @Test
    fun `@Reflective annotation has correct retention`() {
        val annotation = Reflective::class.java.getAnnotation(Retention::class.java)
        assertNotNull(annotation, "Retention annotation should be present")
        assertEquals(AnnotationRetention.RUNTIME, annotation.value, "Should have RUNTIME retention")
    }

    @Test
    fun `@Reflective annotation has correct targets`() {
        val annotation = Reflective::class.java.getAnnotation(Target::class.java)
        assertNotNull(annotation, "Target annotation should be present")

        val targets = annotation.allowedTargets.toSet()
        assertTrue(AnnotationTarget.CLASS in targets, "Should allow CLASS target")
    }

    @Test
    fun `@Reflective can be applied to data classes`() {
        val annotation = TestDataClass::class.java.getAnnotation(Reflective::class.java)
        assertNotNull(annotation, "@Reflective annotation should be present on data class")
    }

    @Test
    fun `@Reflective can be applied to objects`() {
        val annotation = TestObject::class.java.getAnnotation(Reflective::class.java)
        assertNotNull(annotation, "@Reflective annotation should be present on object")
    }

    @Test
    fun `@Reflective can be applied to interfaces`() {
        val annotation = TestInterface::class.java.getAnnotation(Reflective::class.java)
        assertNotNull(annotation, "@Reflective annotation should be present on interface")
    }

    @Test
    fun `@Reflective can be applied to abstract classes`() {
        val annotation = TestAbstractClass::class.java.getAnnotation(Reflective::class.java)
        assertNotNull(annotation, "@Reflective annotation should be present on abstract class")
    }

    @Test
    fun `@Reflective annotation metadata is correctly configured`() {
        // Test that the annotation has the correct configuration
        // instead of using local classes which Portrait doesn't support
        val reflectiveAnnotation = Reflective::class.java

        assertNotNull(reflectiveAnnotation)
        assertTrue(reflectiveAnnotation.isAnnotation)
        assertEquals("Reflective", reflectiveAnnotation.simpleName)
    }
}
