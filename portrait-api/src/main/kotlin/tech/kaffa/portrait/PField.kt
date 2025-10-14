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

    /** True if this field has public visibility */
    abstract val isPublic: Boolean

    /** True if this field has private visibility */
    abstract val isPrivate: Boolean

    /** True if this field has protected visibility */
    abstract val isProtected: Boolean

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
    abstract val annotations: List<PAnnotation>

    /**
     * Gets a specific annotation from this field.
     *
     * @param annotationClass The annotation type to look for
     * @return The annotation if present, null otherwise
     */
    abstract fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation?

    /**
     * Checks if this field has a specific annotation.
     *
     * @param annotationClass The annotation type to check for
     * @return True if the annotation is present
     */
    abstract fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean

    override fun toString(): String =
        "${declaringClass.simpleName}.$name: ${type.simpleName}"
}