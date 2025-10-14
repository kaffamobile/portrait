package kaffa.portrait

/**
 * Abstract representation of a constructor that provides controlled access to reflection capabilities.
 *
 * PConstructor wraps platform-specific constructor representations and provides a unified API
 * for constructor introspection and invocation across different runtime environments.
 *
 * This abstraction allows the same code to work with different reflection implementations
 * (JVM, AOT, etc.) while hiding implementation details from the user.
 *
 * @param T The type that this constructor creates
 */
abstract class PConstructor<T : Any> {

    /** The class that declares this constructor */
    abstract val declaringClass: PClass<T>

    /** List of parameter types in declaration order */
    abstract val parameterTypes: List<PClass<*>>

    /** All annotations present on this constructor, empty if none */
    abstract val annotations: List<PAnnotation>

    /** True if this constructor is public */
    abstract val isPublic: Boolean

    /** True if this constructor is private */
    abstract val isPrivate: Boolean

    /** True if this constructor is protected */
    abstract val isProtected: Boolean

    /**
     * Gets a specific annotation from this constructor.
     *
     * @param annotationClass The annotation type to look for
     * @return The annotation if present, null otherwise
     */
    abstract fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation?

    /**
     * Checks if this constructor has a specific annotation.
     *
     * @param annotationClass The annotation type to check for
     * @return True if the annotation is present
     */
    abstract fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean

    /**
     * Invokes this constructor with the given arguments to create a new instance.
     *
     * @param args The arguments to pass to the constructor, must match parameter types
     * @return A new instance of type T
     * @throws IllegalArgumentException if argument types don't match or argument count is wrong
     * @throws RuntimeException if the constructor throws an exception or is not accessible
     */
    abstract fun call(vararg args: Any?): T

    /**
     * Invokes this constructor with arguments provided as a list.
     *
     * @param args The arguments to pass to the constructor, must match parameter types
     * @return A new instance of type T
     * @throws IllegalArgumentException if argument types don't match or argument count is wrong
     * @throws RuntimeException if the constructor throws an exception or is not accessible
     */
    abstract fun callBy(args: List<Any?>): T

    /**
     * Checks if this constructor can be called with the given argument types.
     *
     * @param argumentTypes The types of arguments to check
     * @return True if the constructor accepts these argument types
     */
    abstract fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean

    override fun toString(): String {
        val params = parameterTypes.joinToString(", ") { it.simpleName }
        return "PConstructor(${declaringClass.simpleName}($params))"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PConstructor<*>) return false

        return declaringClass == other.declaringClass &&
                parameterTypes == other.parameterTypes
    }

    override fun hashCode(): Int {
        var result = declaringClass.hashCode()
        result = 31 * result + parameterTypes.hashCode()
        return result
    }
}