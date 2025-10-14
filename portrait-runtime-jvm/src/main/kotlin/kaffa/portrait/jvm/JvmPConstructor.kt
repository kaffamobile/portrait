package kaffa.portrait.jvm

import kaffa.portrait.PAnnotation
import kaffa.portrait.PClass
import kaffa.portrait.PConstructor
import kaffa.portrait.Portrait
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

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

    override val declaringClass: PClass<T> by lazy { Portrait.of(constructor.declaringClass) }
    override val parameterTypes: List<PClass<*>> by lazy { constructor.parameterTypes.map { Portrait.of(it) } }
    override val isPublic: Boolean = Modifier.isPublic(constructor.modifiers)
    override val isPrivate: Boolean = Modifier.isPrivate(constructor.modifiers)
    override val isProtected: Boolean = Modifier.isProtected(constructor.modifiers)

    override val annotations: List<PAnnotation> =
        constructor.annotations.map { JvmPAnnotation(it) }

    override fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation? {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return constructor.getAnnotation(javaClass)?.let { JvmPAnnotation(it) }
    }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return constructor.isAnnotationPresent(javaClass)
    }

    override fun call(vararg args: Any?): T {
        return constructor.newInstance(*args)
    }

    override fun callBy(args: List<Any?>): T {
        return call(*args.toTypedArray())
    }

    override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
        if (argumentTypes.size != parameterTypes.size) return false
        return parameterTypes.zip(argumentTypes).all { (paramType, argType) ->
            paramType.isAssignableFrom(argType)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is JvmPConstructor<*> && constructor == other.constructor

    override fun hashCode(): Int = constructor.hashCode()
}