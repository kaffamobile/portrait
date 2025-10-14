package kaffa.portrait

/**
 * Abstract representation of an annotation that provides controlled access to annotation values.
 *
 * PAnnotation wraps platform-specific annotation instances and provides a unified API
 * for annotation introspection across different runtime environments.
 */
abstract class PAnnotation {

    /** The Portrait class of this annotation type */
    abstract val annotationClass: PClass<out Annotation>

    /** Simple name of the annotation (without package) */
    abstract val simpleName: String

    /** Fully qualified name of the annotation, or null if anonymous */
    abstract val qualifiedName: String?

    /**
     * Gets the value of an annotation property by name.
     *
     * @param propertyName The name of the annotation property
     * @return The property value, or null if not found or access fails
     */
    abstract fun getValue(propertyName: String): Any?

    /**
     * Gets a string value from the annotation.
     *
     * @param propertyName The name of the annotation property
     * @return The string value, or null if not a string or not found
     */
    fun getStringValue(propertyName: String): String? =
        getValue(propertyName) as? String

    /**
     * Gets a boolean value from the annotation.
     *
     * @param propertyName The name of the annotation property
     * @return The boolean value, or null if not a boolean or not found
     */
    fun getBooleanValue(propertyName: String): Boolean? =
        getValue(propertyName) as? Boolean

    /**
     * Gets an integer value from the annotation.
     *
     * @param propertyName The name of the annotation property
     * @return The integer value, or null if not an integer or not found
     */
    fun getIntValue(propertyName: String): Int? =
        getValue(propertyName) as? Int

    /**
     * Gets a list value from the annotation.
     *
     * @param propertyName The name of the annotation property
     * @return The list value, or null if not a list or not found
     */
    fun getListValue(propertyName: String): List<*>? =
        getValue(propertyName) as? List<*>

    /**
     * Gets a class value from the annotation as PClass.
     *
     * @param propertyName The name of the annotation property
     * @return The class value as PClass, or null if not a PClass or not found
     */
    fun getClassValue(propertyName: String): PClass<*>? =
        getValue(propertyName) as? PClass<*>

    /**
     * Gets a list of class values from the annotation as PClass list.
     *
     * @param propertyName The name of the annotation property
     * @return List of PClass values, or null if not a PClass list or not found
     */
    @Suppress("UNCHECKED_CAST")
    fun getClassListValue(propertyName: String): List<PClass<*>>? =
        getValue(propertyName) as? List<PClass<*>>

    override fun toString(): String = "@$simpleName"
}