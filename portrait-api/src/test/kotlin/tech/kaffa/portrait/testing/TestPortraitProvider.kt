package tech.kaffa.portrait.testing

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PConstructor
import tech.kaffa.portrait.PField
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.TestClass
import tech.kaffa.portrait.provider.PortraitProvider
import tech.kaffa.portrait.proxy.ProxyHandler

/**
 * Simple PortraitProvider used by tests to validate the public API behavior without
 * depending on the runtime-specific implementations.
 *
 * The provider only recognizes [TestClass] and falls back to [UnresolvedPClass] for
 * anything else. This ensures the loading pipeline remains exercised when providers
 * are present while still allowing tests to cover failure paths.
 */
class TestPortraitProvider : PortraitProvider {
    override fun priority(): Int = 250

    override fun <T : Any> forName(className: String): PClass<T>? {
        return when (className) {
            TestClass::class.java.name -> safeCast(TestClassPortrait)
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> safeCast(pClass: PClass<out Any>): PClass<T> {
        return pClass as PClass<T>
    }
}

private object TestClassPortrait : PClass<TestClass>() {
    override val simpleName: String = TestClass::class.java.simpleName
    override val qualifiedName: String = TestClass::class.java.name

    override val isAbstract: Boolean = false
    override val isSealed: Boolean = false
    override val isData: Boolean = false
    override val isCompanion: Boolean = false
    override val isEnum: Boolean = false

    override val objectInstance: TestClass? = null
    override val enumConstants: Array<TestClass>? = null

    override val superclass: PClass<*>? = null
    override val interfaces: List<PClass<*>> = emptyList()

    override val annotations: List<PAnnotation<*>> = emptyList()
    override fun <A : Annotation> getAnnotation(annotationClass: PClass<A>): PAnnotation<A>? = null
    override fun hasAnnotation(annotationClass: PClass<*>): Boolean = false

    override val constructors: List<PConstructor<TestClass>> = emptyList()
    override fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<TestClass>? = null

    override val methods: List<PMethod> = emptyList()
    override fun getMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? = null

    override val fields: List<PField> = emptyList()
    override fun getField(name: String): PField? = null

    override fun newInstance(vararg args: Any?): TestClass {
        return when (args.size) {
            0 -> TestClass()
            1 -> TestClass(args[0] as String)
            else -> throw IllegalArgumentException("Unsupported constructor arguments: ${args.toList()}")
        }
    }

    override fun createProxy(handler: ProxyHandler<TestClass>): TestClass {
        throw UnsupportedOperationException("Test stub does not support proxies")
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        return other.qualifiedName == qualifiedName
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        return other.qualifiedName == qualifiedName
    }
}
