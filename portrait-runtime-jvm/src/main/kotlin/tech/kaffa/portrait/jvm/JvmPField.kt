package tech.kaffa.portrait.jvm

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PField
import tech.kaffa.portrait.Portrait
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * JVM implementation of PField using Java reflection.
 *
 * This class wraps a Java Field and provides Portrait API access to its
 * reflection capabilities using standard JVM reflection under the hood.
 *
 * Internal implementation detail - users should access this through the
 * Portrait API rather than instantiating directly.
 *
 * @param field The Java Field to wrap
 */
internal class JvmPField(private val field: Field) : PField() {

    /**
     * Converts a PClass from any provider to a Java Class.
     * Handles primitive types from WellKnownPortraitProvider and other cross-provider scenarios.
     */
    private fun pClassToJavaClass(pClass: PClass<*>): Class<*> {
        return when {
            // If it's already a JvmPClass, use its Java class directly
            pClass is JvmPClass<*> -> pClass.javaClass

            // Handle primitive types by their qualified name
            pClass.isPrimitive -> {
                when (pClass.qualifiedName) {
                    "boolean" -> Boolean::class.javaPrimitiveType!!
                    "byte" -> Byte::class.javaPrimitiveType!!
                    "char" -> Char::class.javaPrimitiveType!!
                    "short" -> Short::class.javaPrimitiveType!!
                    "int" -> Int::class.javaPrimitiveType!!
                    "long" -> Long::class.javaPrimitiveType!!
                    "float" -> Float::class.javaPrimitiveType!!
                    "double" -> Double::class.javaPrimitiveType!!
                    "void" -> Void::class.javaPrimitiveType!!
                    else -> throw IllegalArgumentException("Unknown primitive type: ${pClass.qualifiedName}")
                }
            }

            // Handle other types by class name lookup
            else -> {
                try {
                    Class.forName(pClass.qualifiedName)
                } catch (e: ClassNotFoundException) {
                    throw IllegalArgumentException("Cannot find Java class for: ${pClass.qualifiedName}", e)
                }
            }
        }
    }

    override val name: String = field.name
    override val type: PClass<*> by lazy { Portrait.of(field.type) }
    override val declaringClass: PClass<*> by lazy { Portrait.of(field.declaringClass) }
    override val isStatic: Boolean = Modifier.isStatic(field.modifiers)
    override val isFinal: Boolean = Modifier.isFinal(field.modifiers)

    override fun get(instance: Any?): Any? {
        return field.get(instance)
    }

    override fun set(instance: Any?, value: Any?) {
        field.set(instance, value)
    }

    override val annotations: List<PAnnotation> =
        field.annotations.map { JvmPAnnotation(it) }

    override fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation? {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return field.getAnnotation(javaClass)?.let { JvmPAnnotation(it) }
    }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return field.isAnnotationPresent(javaClass)
    }

}
