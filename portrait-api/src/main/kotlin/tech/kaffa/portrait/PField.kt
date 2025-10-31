package tech.kaffa.portrait

/**
 * Abstract representation of a field that provides controlled access to field reflection.
 *
 * PField wraps platform-specific field representations and provides a unified API
 * for field introspection and manipulation across different runtime environments.
 */
abstract class PField {

    /** The name of this field */
    abstract val name: String

    /** The type of this field */
    abstract val type: PClass<*>

    /** The class that declares this field */
    abstract val declaringClass: PClass<*>

    /** True if this is a static field */
    abstract val isStatic: Boolean

    /** True if this field is final (cannot be modified after initialization) */
    abstract val isFinal: Boolean


    /**
     * Gets the value of this field from the given instance.
     *
     * @param instance The object to read the field from, or null for static fields
     * @return The field's current value
     * @throws RuntimeException if access fails
     */
    abstract fun get(instance: Any?): Any?

    /**
     * Sets the value of this field on the given instance.
     *
     * @param instance The object to modify, or null for static fields
     * @param value The new value to set
     * @throws RuntimeException if access fails or field is final
     */
    abstract fun set(instance: Any?, value: Any?)

    /** All annotations present on this field, empty if none */
    abstract val annotations: List<PAnnotation<*>>

    /**
     * Gets a specific annotation from this field.
     *
     * @param annotationClass The annotation type to look for
     * @return The annotation if present, null otherwise
     */
    abstract fun <A : Annotation> getAnnotation(annotationClass: PClass<A>): PAnnotation<A>?

    /**
     * Checks if this field has a specific annotation.
     *
     * @param annotationClass The annotation type to check for
     * @return True if the annotation is present
     */
    abstract fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PField) return false

        if (declaringClass.qualifiedName != other.declaringClass.qualifiedName) return false
        if (name != other.name) return false
        if (type.qualifiedName != other.type.qualifiedName) return false

        val flagsMatch = isStatic == other.isStatic &&
            isFinal == other.isFinal
        if (!flagsMatch) return false

        if (annotations.size != other.annotations.size) return false

        return true
    }

    override fun hashCode(): Int {
        fun mix(seed: Int, value: Int): Int = seed * 31 + value

        var result = declaringClass.qualifiedName.hashCode()
        result = mix(result, name.hashCode())
        result = mix(result, type.qualifiedName.hashCode())

        val flags = (if (isStatic) 1 else 0) or
            (if (isFinal) 1 shl 1 else 0)
        result = mix(result, flags)

        result = mix(result, annotations.size)

        return result
    }

    override fun toString(): String =
        "${declaringClass.simpleName}.$name: ${type.simpleName}"
}
