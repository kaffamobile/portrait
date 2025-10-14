package kaffa.portrait

import kaffa.portrait.proxy.ProxyHandler

/**
 * Abstract representation of a class that provides controlled access to reflection capabilities.
 *
 * PClass wraps platform-specific class representations and provides a unified API
 * for type introspection across different runtime environments.
 *
 * This abstraction allows the same code to work with different reflection implementations
 * (JVM, AOT, etc.) while hiding implementation details from the user.
 *
 * @param T The type represented by this PClass
 */
abstract class PClass<T : Any> {

    /** Simple name of the class (without package) */
    abstract val simpleName: String

    /** Fully qualified name of the class, or null if anonymous */
    abstract val qualifiedName: String?

    /** True if this class is abstract */
    abstract val isAbstract: Boolean

    /** True if this is a Kotlin sealed class */
    abstract val isSealed: Boolean

    /** True if this is a Kotlin data class */
    abstract val isData: Boolean

    /** True if this is a Kotlin companion object */
    abstract val isCompanion: Boolean

    /** The singleton instance if this is a Kotlin object, null otherwise */
    abstract val objectInstance: T?


    /** The direct superclass of this class, or null if this is Any/Object or an interface */
    abstract val superclass: PClass<*>?

    /** List of interfaces directly implemented by this class */
    abstract val interfaces: List<PClass<*>>

    /**
     * Creates a new instance of this class using a constructor with the given arguments.
     * This is a convenience method that finds and calls the appropriate constructor.
     *
     * @param args The arguments to pass to the constructor
     * @return A new instance of type T
     * @throws IllegalArgumentException if no matching constructor is found
     * @throws RuntimeException if the constructor throws an exception
     */
    abstract fun createInstance(vararg args: Any?): T

    /**
     * Checks if this class is assignable from another class.
     *
     * @param other The other class to check
     * @return True if instances of other can be assigned to variables of this type
     */
    abstract fun isAssignableFrom(other: PClass<*>): Boolean

    /**
     * Checks if this class is a subclass of another class.
     *
     * @param other The potential superclass
     * @return True if this class extends or implements other
     */
    abstract fun isSubclassOf(other: PClass<*>): Boolean

    /** All annotations present on this class, empty if none */
    abstract val annotations: List<PAnnotation>

    /**
     * Gets a specific annotation from this class.
     *
     * @param annotationClass The annotation type to look for
     * @return The annotation if present, null otherwise
     */
    abstract fun getAnnotation(annotationClass: PClass<*>): PAnnotation?

    /**
     * Checks if this class has a specific annotation.
     *
     * @param annotationClass The annotation type to check for
     * @return True if the annotation is present
     */
    abstract fun hasAnnotation(annotationClass: PClass<*>): Boolean

    /** All constructors declared in this class */
    abstract val constructors: List<PConstructor<T>>

    /**
     * Gets a specific constructor by parameter types.
     *
     * @param parameterTypes The parameter types in order
     * @return The constructor if found, null otherwise
     */
    abstract fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>?

    /** All methods declared directly in this class (not inherited) */
    abstract val declaredMethods: List<PMethod>

    /**
     * Gets a specific public method by name and parameter types.
     *
     * @param name The method name
     * @param parameterTypes The parameter types in order
     * @return The method if found, null otherwise
     */
    abstract fun getDeclaredMethod(name: String, vararg parameterTypes: PClass<*>): PMethod?

    /** All fields declared directly in this class (not inherited) */
    abstract val declaredFields: List<PField>

    /**
     * Gets a specific field declared in this class by name.
     *
     * @param name The field name
     * @return The field if found, null otherwise
     */
    abstract fun getDeclaredField(name: String): PField?

    /**
     * Creates a dynamic proxy instance that implements this class/interface.
     *
     * This method creates a proxy object that intercepts all method calls and delegates
     * them to the provided ProxyHandler. The proxy can be used to dynamically implement
     * interfaces or extend abstract classes at runtime.
     *
     * Requirements:
     * - The target type must be an interface or abstract class
     * - All abstract methods must be handled by the ProxyHandler
     * - The proxy implements the exact type represented by this PClass
     *
     * Example usage:
     * ```kotlin
     * interface Calculator {
     *     fun add(a: Int, b: Int): Int
     *     fun multiply(a: Int, b: Int): Int
     * }
     *
     * val calculatorClass = Calculator::class.java.portrait
     * val proxy = calculatorClass.createProxy { self, method, args ->
     *     when (method.name) {
     *         "add" -> (args[0] as Int) + (args[1] as Int)
     *         "multiply" -> (args[0] as Int) * (args[1] as Int)
     *         else -> throw UnsupportedOperationException("Unknown method: ${method.name}")
     *     }
     * }
     *
     * println(proxy.add(2, 3))      // Output: 5
     * println(proxy.multiply(4, 5)) // Output: 20
     * ```
     *
     * @param handler The ProxyHandler to handle method invocations
     * @return A proxy instance of type T
     * @throws ProxyCreationException if the proxy cannot be created
     * @throws IllegalArgumentException if the type is not suitable for proxying
     */
    abstract fun createProxy(handler: ProxyHandler<T>): T

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PClass<*>) return false
        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        return qualifiedName?.hashCode() ?: 0
    }

    override fun toString(): String = "PClass($qualifiedName)"

    open val isPrimitive = false
}