package tech.kaffa.portrait.jvm

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.Portrait
import java.lang.reflect.Method

/**
 * JVM implementation of PMethod using Java reflection.
 *
 * This class wraps a Java Method and provides Portrait API access to its
 * reflection capabilities using standard JVM reflection under the hood.
 *
 * Internal implementation detail - users should access this through the
 * Portrait API rather than instantiating directly.
 *
 * @param method The Java Method to wrap
 */
internal class JvmPMethod(private val method: Method) : PMethod() {

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
                val className = pClass.qualifiedName
                    ?: throw IllegalArgumentException("Cannot determine class name for PClass: $pClass")
                try {
                    Class.forName(className)
                } catch (e: ClassNotFoundException) {
                    throw IllegalArgumentException("Cannot find Java class for: $className", e)
                }
            }
        }
    }

    override val name: String = method.name
    override val parameterTypes: List<PClass<*>> by lazy { method.parameterTypes.map { Portrait.of(it) } }
    override val parameterCount: Int = method.parameterCount
    override val returnType: PClass<*> by lazy { Portrait.of(method.returnType) }
    override val declaringClass: PClass<*> by lazy { Portrait.of(method.declaringClass) }
    override val isPublic: Boolean = java.lang.reflect.Modifier.isPublic(method.modifiers)
    override val isPrivate: Boolean = java.lang.reflect.Modifier.isPrivate(method.modifiers)
    override val isProtected: Boolean = java.lang.reflect.Modifier.isProtected(method.modifiers)
    override val isStatic: Boolean = java.lang.reflect.Modifier.isStatic(method.modifiers)
    override val isFinal: Boolean = java.lang.reflect.Modifier.isFinal(method.modifiers)
    override val isAbstract: Boolean = java.lang.reflect.Modifier.isAbstract(method.modifiers)

    override fun invoke(instance: Any?, vararg args: Any?): Any? {
        if (!method.isAccessible) {
            method.isAccessible = true
        }
        return method.invoke(instance, *args)
    }

    override val annotations: List<PAnnotation> =
        method.annotations.map { JvmPAnnotation(it) }

    override fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation? {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return method.getAnnotation(javaClass)?.let { JvmPAnnotation(it) }
    }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return method.isAnnotationPresent(javaClass)
    }

    override val parameterAnnotations: List<List<PAnnotation>> =
        method.parameterAnnotations.map { paramAnnotations ->
            paramAnnotations.map { JvmPAnnotation(it) }
        }

    override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
        if (argumentTypes.size != parameterTypes.size) return false

        return parameterTypes.zip(argumentTypes.toList()).all { (paramType, argType) ->
            paramType.isAssignableFrom(argType)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is JvmPMethod && method == other.method

    override fun hashCode(): Int = method.hashCode()
}