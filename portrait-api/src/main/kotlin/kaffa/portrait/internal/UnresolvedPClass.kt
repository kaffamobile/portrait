package kaffa.portrait.internal

import kaffa.portrait.PAnnotation
import kaffa.portrait.PClass
import kaffa.portrait.PConstructor
import kaffa.portrait.PField
import kaffa.portrait.PMethod
import kaffa.portrait.proxy.ProxyHandler

/**
 * PClass implementation for types that could not be resolved by any provider.
 *
 * This represents a type that is referenced in metadata or code but for which
 * no actual type information is available. This commonly happens in AOT scenarios
 * where metadata references types that weren't included in the AOT compilation.
 *
 * UnresolvedPClass provides safe fallback behavior, allowing the system to continue
 * operating even when some type information is missing, rather than failing completely.
 *
 * @param T The type that this PClass is supposed to represent
 * @param className The name of the unresolved class
 */
class UnresolvedPClass<T : Any>(
    private val className: String
) : PClass<T>() {

    override val simpleName: String = className.substringAfterLast('.')
    override val qualifiedName: String = className
    override val isAbstract: Boolean = false // Unknown, assume false
    override val isSealed: Boolean = false // Unknown, assume false  
    override val isData: Boolean = false // Unknown, assume false
    override val isCompanion: Boolean = false // Unknown, assume false
    override val objectInstance: T? = null // Unknown, assume null
    override val superclass: PClass<*>? = null // Unknown, could cause issues but safer than guessing
    override val interfaces: List<PClass<*>> = emptyList() // Unknown, assume none

    override fun createInstance(vararg args: Any?): T {
        throw UnsupportedOperationException("Cannot create instance of unresolved type: $className")
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        // Conservative approach: only match exact type names
        return other.qualifiedName == this.qualifiedName
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        // Conservative approach: assume no inheritance relationships
        return false
    }

    override val annotations: List<PAnnotation> = emptyList()
    override fun getAnnotation(annotationClass: PClass<*>): PAnnotation? = null
    override fun hasAnnotation(annotationClass: PClass<*>): Boolean = false

    override val constructors: List<PConstructor<T>> = emptyList()
    override fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>? = null

    override val declaredMethods: List<PMethod> = emptyList()
    override fun getDeclaredMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? = null

    override val declaredFields: List<PField> = emptyList()
    override fun getDeclaredField(name: String): PField? = null

    override fun createProxy(handler: ProxyHandler<T>): T {
        throw UnsupportedOperationException("Cannot create proxy for unresolved type: $className")
    }

    override fun toString(): String = "UnresolvedPClass($className)"

    /**
     * Indicates that this PClass represents an unresolved type.
     * Can be used by calling code to detect and handle unresolved types specially.
     */
    val isUnresolved: Boolean = true
}