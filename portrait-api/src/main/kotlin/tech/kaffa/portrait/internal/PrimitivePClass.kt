package tech.kaffa.portrait.internal

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PConstructor
import tech.kaffa.portrait.PField
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.proxy.ProxyHandler

/**
 * PClass implementation for primitive types.
 */
internal class PrimitivePClass<T : Any>(primitiveTypeName: String) : PClass<T>() {
    private val wrapperTypeName = BoxedPrimitives.boxing[primitiveTypeName]

    override val simpleName: String = primitiveTypeName
    override val qualifiedName: String = primitiveTypeName
    override val isAbstract: Boolean = false
    override val isSealed: Boolean = false
    override val isData: Boolean = false
    override val isCompanion: Boolean = false
    override val objectInstance: T? = null
    override val superclass: PClass<*>? = null
    override val interfaces: List<PClass<*>> = emptyList()
    override val isPrimitive = true

    override fun createInstance(vararg args: Any?): T {
        throw UnsupportedOperationException("Cannot create instance of primitive type $simpleName")
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        return other.qualifiedName == this.qualifiedName || other.qualifiedName == wrapperTypeName
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        return false
    }

    override val annotations: List<PAnnotation> = emptyList()
    override fun getAnnotation(annotationClass: PClass<*>): PAnnotation? = null
    override fun hasAnnotation(annotationClass: PClass<*>): Boolean = false

    override val constructors: List<PConstructor<T>> = emptyList()
    override fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>? = null

    override val methods: List<PMethod> = emptyList()
    override fun getMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? = null

    override val fields: List<PField> = emptyList()
    override fun getField(name: String): PField? = null

    override fun createProxy(handler: ProxyHandler<T>): T {
        throw UnsupportedOperationException("Cannot create proxy for primitive type $simpleName")
    }

    override fun toString(): String = "PClass[primitive](${qualifiedName})"
}
