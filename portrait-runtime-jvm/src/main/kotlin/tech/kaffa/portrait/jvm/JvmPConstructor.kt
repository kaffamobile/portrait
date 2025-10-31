package tech.kaffa.portrait.jvm

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PConstructor
import tech.kaffa.portrait.Portrait
import java.lang.reflect.Constructor

/**
 * JVM implementation of PConstructor using Java reflection.
 *
 * This class wraps a Java Constructor and provides Portrait API access to its
 * reflection capabilities using standard JVM reflection under the hood.
 *
 * Internal implementation detail - users should access this through the
 * Portrait API rather than instantiating directly.
 *
 * @param constructor The Java Constructor to wrap
 */
internal class JvmPConstructor<T : Any>(private val constructor: Constructor<T>) : PConstructor<T>() {

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

    override val declaringClass: PClass<T> by lazy { Portrait.of(constructor.declaringClass) }
    override val parameterTypes: List<PClass<*>> by lazy { constructor.parameterTypes.map { Portrait.of(it) } }
    override val annotations: List<PAnnotation<*>> =
        constructor.annotations.map { JvmPAnnotation(it) }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation> getAnnotation(annotationClass: PClass<A>): PAnnotation<A>? {
        val javaClass = pClassToJavaClass(annotationClass) as Class<A>
        val instance = constructor.getAnnotation(javaClass) ?: return null
        return JvmPAnnotation(instance)
    }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return constructor.isAnnotationPresent(javaClass)
    }

    override fun newInstance(vararg args: Any?): T {
        return constructor.newInstance(*args)
    }

    override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
        if (argumentTypes.size != parameterTypes.size) return false
        return parameterTypes.zip(argumentTypes).all { (paramType, argType) ->
            paramType.isAssignableFrom(argType)
        }
    }

}
