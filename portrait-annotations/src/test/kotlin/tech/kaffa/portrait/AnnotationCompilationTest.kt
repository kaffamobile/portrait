package tech.kaffa.portrait

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ProxyTarget
@Reflective
interface ComplexInterface {
    fun doSomething(): String
}

@Reflective
data class KotlinDataClass(
    val id: Int,
    val name: String
) {
    companion object {
        const val DEFAULT_NAME = "default"
    }
}

@ProxyTarget
interface KotlinInterface {
    fun processData(data: KotlinDataClass): String
    fun getName(): String {
        return "default"
    } // default implementation
}

@Reflective
class GenericClass<T> {
    fun process(value: T): T = value
}

@ProxyTarget
interface GenericInterface<in T, out R> {
    fun transform(input: T): R
}

@Reflective
abstract class AbstractBase {
    abstract fun abstractMethod(): String
}

@Reflective
class ConcreteImplementation : AbstractBase() {
    override fun abstractMethod(): String = "implemented"
}

@Reflective(including = [Includes.PUBLIC_API])
class PublicApiRoot {
    fun createExternal(): ExternalClass = ExternalClass()
}

@Reflective(including = [Includes.PUBLIC_API, Includes.PUBLIC_API_SUPERTYPES, Includes.PUBLIC_API_SUBTYPES])
class PublicApiDirectionalRoot {
    val instance: ExternalClass = ExternalClass()
    fun provideInterface(): InterfaceBase = throw UnsupportedOperationException("stub")
}

@ProxyTarget
interface InterfaceBase {
    fun baseMethod(): String
}

@ProxyTarget
interface ExtendedInterface : InterfaceBase {
    fun extendedMethod(): String
}

// External classes for include testing
class ExternalClass {
    fun externalMethod(): String = "external"
}

interface ExternalInterface {
    fun externalInterfaceMethod(): String
}

// Classes that use the new Include annotations
@Reflective.Include(classes = [ExternalClass::class], including = [Includes.DIRECT_SUBTYPES])
@ProxyTarget.Include(classes = [ExternalInterface::class], including = [Includes.ALL_SUBTYPES])
class ConfigurationClass

/**
 * Tests to ensure annotations compile correctly and don't break compilation.
 */
class AnnotationCompilationTest {

    @Test
    fun `all annotations can be used together`() {
        val reflectiveAnnotation = ComplexInterface::class.java.getAnnotation(Reflective::class.java)
        val proxyAnnotation = ComplexInterface::class.java.getAnnotation(ProxyTarget::class.java)

        assertNotNull(reflectiveAnnotation, "@Reflective should be present")
        assertNotNull(proxyAnnotation, "@ProxyTarget should be present")
    }

    @Test
    fun `annotations work with Kotlin features`() {
        val dataAnnotation = KotlinDataClass::class.java.getAnnotation(Reflective::class.java)
        val interfaceAnnotation = KotlinInterface::class.java.getAnnotation(ProxyTarget::class.java)

        assertNotNull(dataAnnotation, "@Reflective should work with data classes")
        assertNotNull(interfaceAnnotation, "@ProxyTarget should work with interfaces with default methods")
    }

    @Test
    fun `annotations work with generics`() {
        val classAnnotation = GenericClass::class.java.getAnnotation(Reflective::class.java)
        val interfaceAnnotation = GenericInterface::class.java.getAnnotation(ProxyTarget::class.java)

        assertNotNull(classAnnotation, "@Reflective should work with generic classes")
        assertNotNull(interfaceAnnotation, "@ProxyTarget should work with generic interfaces")
    }

    @Test
    fun `annotations work with inheritance`() {
        val baseAnnotation = AbstractBase::class.java.getAnnotation(Reflective::class.java)
        val concreteAnnotation = ConcreteImplementation::class.java.getAnnotation(Reflective::class.java)
        val interfaceBaseAnnotation = InterfaceBase::class.java.getAnnotation(ProxyTarget::class.java)
        val extendedAnnotation = ExtendedInterface::class.java.getAnnotation(ProxyTarget::class.java)

        assertNotNull(baseAnnotation, "@Reflective should work with abstract base classes")
        assertNotNull(concreteAnnotation, "@Reflective should work with concrete implementations")
        assertNotNull(interfaceBaseAnnotation, "@ProxyTarget should work with base interfaces")
        assertNotNull(extendedAnnotation, "@ProxyTarget should work with extended interfaces")
    }

    @Test
    fun `Include annotations work correctly`() {
        val reflectiveInclude = ConfigurationClass::class.java.getAnnotationsByType(Reflective.Include::class.java)
        val proxyInclude = ConfigurationClass::class.java.getAnnotationsByType(ProxyTarget.Include::class.java)

        assertNotNull(reflectiveInclude, "@Reflective.Include should be present")
        assertNotNull(proxyInclude, "@ProxyTarget.Include should be present")

        assertTrue(reflectiveInclude.isNotEmpty(), "@Reflective.Include should have annotations")
        assertTrue(proxyInclude.isNotEmpty(), "@ProxyTarget.Include should have annotations")

        // Check that the classes array is properly set
        val reflectiveClasses = reflectiveInclude[0].classes
        val proxyClasses = proxyInclude[0].classes

        assertTrue(reflectiveClasses.contains(ExternalClass::class), "Should include ExternalClass")
        assertTrue(proxyClasses.contains(ExternalInterface::class), "Should include ExternalInterface")

        // Check that the including array is properly set
        val reflectiveIncluding = reflectiveInclude[0].including
        val proxyIncluding = proxyInclude[0].including

        assertTrue(reflectiveIncluding.contains(Includes.DIRECT_SUBTYPES), "Should include DIRECT_SUBTYPES")
        assertTrue(proxyIncluding.contains(Includes.ALL_SUBTYPES), "Should include ALL_SUBTYPES")
    }

    @Test
    fun `public api include is supported`() {
        val reflectiveAnnotation = PublicApiRoot::class.java.getAnnotation(Reflective::class.java)

        assertNotNull(reflectiveAnnotation, "@Reflective should be present on PublicApiRoot")
        assertTrue(
            reflectiveAnnotation.including.contains(Includes.PUBLIC_API),
            "Including should contain PUBLIC_API"
        )
    }

    @Test
    fun `public api directional includes are supported`() {
        val reflectiveAnnotation = PublicApiDirectionalRoot::class.java.getAnnotation(Reflective::class.java)

        assertNotNull(reflectiveAnnotation, "@Reflective should be present on PublicApiDirectionalRoot")
        assertTrue(
            reflectiveAnnotation.including.contains(Includes.PUBLIC_API_SUPERTYPES),
            "Including should contain PUBLIC_API_SUPERTYPES"
        )
        assertTrue(
            reflectiveAnnotation.including.contains(Includes.PUBLIC_API_SUBTYPES),
            "Including should contain PUBLIC_API_SUBTYPES"
        )
    }
}

