package tech.kaffa.portrait

import kotlin.test.Test
import tech.kaffa.portrait.proxy.ProxyHandler
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PMethodMatchesTest {

    private val declaringType = fakeClass("tech.kaffa.portrait.FakeDeclaring")

    @Test
    fun `matches returns true when name and parameter types align`() {
        val method = fakeMethod(
            name = "process",
            parameterTypes = listOf(fakeClass("java.lang.String"), fakeClass("int")),
            returnType = fakeClass("java.lang.Boolean")
        )

        val result = method.matches(
            "process",
            fakeClass("java.lang.String"),
            fakeClass("int")
        )

        assertTrue(result)
    }

    @Test
    fun `matches returns false when name differs`() {
        val method = fakeMethod(
            name = "expected",
            parameterTypes = listOf(fakeClass("java.lang.String")),
            returnType = fakeClass("java.lang.Boolean")
        )

        assertFalse(method.matches("other", fakeClass("java.lang.String")))
    }

    @Test
    fun `matches returns false when parameter count differs`() {
        val method = fakeMethod(
            name = "process",
            parameterTypes = listOf(fakeClass("java.lang.String")),
            returnType = fakeClass("java.lang.Boolean")
        )

        assertFalse(
            method.matches(
                "process",
                fakeClass("java.lang.String"),
                fakeClass("int")
            )
        )
    }

    @Test
    fun `matches returns false when any parameter type differs`() {
        val method = fakeMethod(
            name = "process",
            parameterTypes = listOf(fakeClass("java.lang.String"), fakeClass("int")),
            returnType = fakeClass("java.lang.Boolean")
        )

        assertFalse(
            method.matches(
                "process",
                fakeClass("java.lang.String"),
                fakeClass("long")
            )
        )
    }

    private fun fakeMethod(
        name: String,
        parameterTypes: List<PClass<*>>,
        returnType: PClass<*>
    ): PMethod {
        return object : PMethod() {
            override val name: String = name
            override val parameterTypes: List<PClass<*>> = parameterTypes
            override val parameterCount: Int = parameterTypes.size
            override val returnType: PClass<*> = returnType
            override val declaringClass: PClass<*> = declaringType
            override val isPublic: Boolean = true
            override val isPrivate: Boolean = false
            override val isProtected: Boolean = false
            override val isStatic: Boolean = false
            override val isFinal: Boolean = false
            override val isAbstract: Boolean = false
            override val annotations: List<PAnnotation> = emptyList()
            override val parameterAnnotations: List<List<PAnnotation>> =
                parameterTypes.map { emptyList() }

            override fun invoke(instance: Any?, vararg args: Any?): Any? {
                throw UnsupportedOperationException("Not used in tests")
            }

            override fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation? = null

            override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean = false

            override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
                throw UnsupportedOperationException("Not used in tests")
            }
        }
    }

    private fun fakeClass(qualifiedName: String): PClass<Any> {
        return object : PClass<Any>() {
            override val simpleName: String = qualifiedName.substringAfterLast('.')
            override val qualifiedName: String = qualifiedName
            override val isAbstract: Boolean = false
            override val isSealed: Boolean = false
            override val isData: Boolean = false
            override val isCompanion: Boolean = false
            override val objectInstance: Any? = null
            override val superclass: PClass<*>? = null
            override val interfaces: List<PClass<*>> = emptyList()
            override val annotations: List<PAnnotation> = emptyList()
            override val constructors: List<PConstructor<Any>> = emptyList()
            override val methods: List<PMethod> = emptyList()
            override val fields: List<PField> = emptyList()

            override fun createProxy(handler: ProxyHandler<Any>): Any {
                throw UnsupportedOperationException("Not used in tests")
            }
        }
    }
}

