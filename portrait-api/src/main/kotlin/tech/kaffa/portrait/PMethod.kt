package tech.kaffa.portrait

/**
 * Abstract representation of a method that provides controlled access to method reflection.
 *
 * PMethod wraps platform-specific method representations and provides a unified API
 * for method introspection and invocation across different runtime environments.
 */
abstract class PMethod {

    /** The name of this method */
    abstract val name: String

    /** List of parameter types in declaration order */
    abstract val parameterTypes: List<PClass<*>>

    /** Number of parameters this method accepts */
    abstract val parameterCount: Int

    /** The return type of this method */
    abstract val returnType: PClass<*>

    /** The class that declares this method */
    abstract val declaringClass: PClass<*>

    /** True if this method has public visibility */
    abstract val isPublic: Boolean

    /** True if this method has private visibility */
    abstract val isPrivate: Boolean

    /** True if this method has protected visibility */
    abstract val isProtected: Boolean

    /** True if this is a static method */
    abstract val isStatic: Boolean

    /** True if this method is final (cannot be overridden) */
    abstract val isFinal: Boolean

    /** True if this is an abstract method */
    abstract val isAbstract: Boolean

    /**
     * Invokes this method on the given instance with the provided arguments.
     *
     * @param instance The object to invoke the method on, or null for static methods
     * @param args The arguments to pass to the method
     * @return The method's return value, or null if void
     * @throws RuntimeException if invocation fails
     */
    abstract fun invoke(instance: Any?, vararg args: Any?): Any?

    /** All annotations present on this method, empty if none */
    abstract val annotations: List<PAnnotation>

    /**
     * Gets a specific annotation from this method.
     *
     * @param annotationClass The annotation type to look for
     * @return The annotation if present, null otherwise
     */
    abstract fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation?

    /**
     * Checks if this method has a specific annotation.
     *
     * @param annotationClass The annotation type to check for
     * @return True if the annotation is present
     */
    abstract fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean

    /** Annotations for each parameter of this method */
    abstract val parameterAnnotations: List<List<PAnnotation>>

    /**
     * Checks if this method can be called with the given argument types.
     *
     * @param argumentTypes The types of arguments to check
     * @return True if the method accepts these argument types
     */
    abstract fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean

    /**
     * Checks if this method matches the given signature.
     *
     * @param name The method name to match
     * @param parameterTypes The parameter types to match
     * @return True if name and parameter types match exactly
     */
    fun matches(name: String, vararg parameterTypes: PClass<*>): Boolean {
        if (this.name != name) return false
        if (this.parameterTypes.size != parameterTypes.size) return false
        return this.parameterTypes.zip(parameterTypes).all { (actual, expected) ->
            actual == expected
        }
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PMethod) return false

        if (declaringClass.qualifiedName != other.declaringClass.qualifiedName) return false
        if (name != other.name) return false
        if (returnType.qualifiedName != other.returnType.qualifiedName) return false

        if (parameterTypes.size != other.parameterTypes.size) return false
        val parametersMatch = parameterTypes.zip(other.parameterTypes).all { (actual, expected) ->
            actual.qualifiedName == expected.qualifiedName
        }
        if (!parametersMatch) return false

        val flagsMatch = isPublic == other.isPublic &&
            isPrivate == other.isPrivate &&
            isProtected == other.isProtected &&
            isStatic == other.isStatic &&
            isFinal == other.isFinal &&
            isAbstract == other.isAbstract
        if (!flagsMatch) return false

        if (annotations.size != other.annotations.size) return false
        if (parameterAnnotations.size != other.parameterAnnotations.size) return false

        return true
    }

    override fun hashCode(): Int {
        fun mix(seed: Int, value: Int): Int = seed * 31 + value

        var result = declaringClass.qualifiedName.hashCode()
        result = mix(result, name.hashCode())
        result = mix(result, returnType.qualifiedName.hashCode())

        result = mix(result, parameterTypes.size)
        for (parameter in parameterTypes) {
            result = mix(result, parameter.qualifiedName.hashCode())
        }

        val flags = (if (isPublic) 1 else 0) or
            (if (isPrivate) 1 shl 1 else 0) or
            (if (isProtected) 1 shl 2 else 0) or
            (if (isStatic) 1 shl 3 else 0) or
            (if (isFinal) 1 shl 4 else 0) or
            (if (isAbstract) 1 shl 5 else 0)
        result = mix(result, flags)

        result = mix(result, annotations.size)
        result = mix(result, parameterAnnotations.size)

        return result
    }

    override fun toString(): String =
        "${declaringClass.simpleName}.$name(${parameterTypes.joinToString { it.simpleName }}): ${returnType.simpleName}"
}
