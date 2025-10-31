package tech.kaffa.portrait.jvm

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.PType
import java.lang.reflect.Method
import java.lang.reflect.Modifier

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
                try {
                    Class.forName(pClass.qualifiedName)
                } catch (e: ClassNotFoundException) {
                    throw IllegalArgumentException("Cannot find Java class for: ${pClass.qualifiedName}", e)
                }
            }
        }
    }

    override val name: String = method.name
    override val parameterTypes: List<PClass<*>> by lazy { method.parameterTypes.map { Portrait.of(it) } }
    override val parameterCount: Int = method.parameterCount
    override val returnType: PClass<*> by lazy { Portrait.of(method.returnType) }
    override val genericReturnType: PType by lazy { method.genericReturnType.toPortraitType() }
    override val declaringClass: PClass<*> by lazy { Portrait.of(method.declaringClass) }
    override val isStatic: Boolean = Modifier.isStatic(method.modifiers)
    override val isFinal: Boolean = Modifier.isFinal(method.modifiers)
    override val isAbstract: Boolean = Modifier.isAbstract(method.modifiers)

    override fun invoke(instance: Any?, vararg args: Any?): Any? {
        return try {
            method.invoke(instance, *args)
        } catch (exception: java.lang.reflect.InvocationTargetException) {
            val cause = exception.cause ?: exception
            if (cause is RuntimeException) throw cause
            if (cause is Error) throw cause
            throw cause
        }
    }

    override val annotations: List<PAnnotation<*>> =
        method.annotations.map { JvmPAnnotation(it) }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation> getAnnotation(annotationClass: PClass<A>): PAnnotation<A>? {
        val javaClass = pClassToJavaClass(annotationClass) as Class<A>
        val instance = method.getAnnotation(javaClass) ?: return null
        return JvmPAnnotation(instance)
    }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val javaClass = pClassToJavaClass(annotationClass) as Class<out Annotation>
        return method.isAnnotationPresent(javaClass)
    }

    override val parameterAnnotations: List<List<PAnnotation<*>>> =
        method.parameterAnnotations.map { paramAnnotations ->
            paramAnnotations.map { JvmPAnnotation(it) }
        }

    override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
        if (argumentTypes.size != parameterTypes.size) return false

        return parameterTypes.zip(argumentTypes.toList()).all { (paramType, argType) ->
            paramType.isAssignableFrom(argType)
        }
    }

}
