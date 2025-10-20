package tech.kaffa.portrait

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Top-level test interfaces and classes
@ProxyTarget
interface ProxyTestInterface

@ProxyTarget
class ProxyTestClass

@ProxyTarget
@Reflective
interface CombinedTestInterface {
    fun doSomething(): String
}

@ProxyTarget
fun interface FunctionalTestInterface {
    fun apply(value: String): String
}

@ProxyTarget
interface GenericTestInterface<T> {
    fun process(value: T): T
}

@ProxyTarget
abstract class AbstractTestClass {
    abstract fun abstractMethod(): String
    open fun concreteMethod(): String = "concrete"
}

class ProxyTargetTest {

    @Test
    fun `@ProxyTarget annotation can be applied to interfaces`() {
        val annotation = ProxyTestInterface::class.java.getAnnotation(ProxyTarget::class.java)
        assertNotNull(annotation, "@ProxyTarget annotation should be present on interface")
    }

    @Test
    fun `@ProxyTarget annotation can be applied to classes`() {
        val annotation = ProxyTestClass::class.java.getAnnotation(ProxyTarget::class.java)
        assertNotNull(annotation, "@ProxyTarget annotation should be present on class")
    }

    @Test
    fun `@ProxyTarget annotation has correct retention`() {
        val annotation = ProxyTarget::class.java.getAnnotation(Retention::class.java)
        assertNotNull(annotation, "Retention annotation should be present")
        assertEquals(AnnotationRetention.RUNTIME, annotation.value, "Should have RUNTIME retention")
    }

    @Test
    fun `@ProxyTarget annotation has correct targets`() {
        val annotation = ProxyTarget::class.java.getAnnotation(Target::class.java)
        assertNotNull(annotation, "Target annotation should be present")

        val targets = annotation.allowedTargets.toSet()
        assertTrue(AnnotationTarget.CLASS in targets, "Should allow CLASS target")
    }

    @Test
    fun `@ProxyTarget can be combined with @Reflective`() {
        val proxyAnnotation = CombinedTestInterface::class.java.getAnnotation(ProxyTarget::class.java)
        val reflectiveAnnotation = CombinedTestInterface::class.java.getAnnotation(Reflective::class.java)

        assertNotNull(proxyAnnotation, "@ProxyTarget annotation should be present")
        assertNotNull(reflectiveAnnotation, "@Reflective annotation should be present")
    }

    @Test
    fun `@ProxyTarget can be applied to functional interfaces`() {
        val annotation = FunctionalTestInterface::class.java.getAnnotation(ProxyTarget::class.java)
        assertNotNull(annotation, "@ProxyTarget annotation should be present on functional interface")
    }

    @Test
    fun `@ProxyTarget can be applied to interfaces with generic parameters`() {
        val annotation = GenericTestInterface::class.java.getAnnotation(ProxyTarget::class.java)
        assertNotNull(annotation, "@ProxyTarget annotation should be present on generic interface")
    }

    @Test
    fun `@ProxyTarget can be applied to abstract classes`() {
        val annotation = AbstractTestClass::class.java.getAnnotation(ProxyTarget::class.java)
        assertNotNull(annotation, "@ProxyTarget annotation should be present on abstract class")
    }
}
