package tech.kaffa.portrait.jvm

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PConstructor
import tech.kaffa.portrait.PField
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.internal.BoxedPrimitives
import tech.kaffa.portrait.proxy.ProxyCreationException
import tech.kaffa.portrait.proxy.ProxyHandler
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * JVM implementation of PClass using Kotlin reflection and Java reflection.
 *
 * This class wraps a Kotlin KClass and provides Portrait API access to its
 * reflection capabilities using standard JVM reflection under the hood.
 *
 * Internal implementation detail - users should access this through the
 * Portrait API rather than instantiating directly.
 *
 * @param kClass The Kotlin class to wrap
 */
internal class JvmPClass<T : Any>(private val kClass: KClass<T>) : PClass<T>() {

    // Internal access to the Java class for use by other JVM reflection classes
    internal val javaClass: Class<T> get() = kClass.java

    /**
     * Converts a PClass from any provider to a Java Class.
     * Handles primitive types from WellKnownPortraitProvider and other cross-provider scenarios.
     */
    private fun pClassToJavaClass(pClass: PClass<*>): Class<*> {
        return when {
            // If it's already a JvmPClass, use its Java class directly
            pClass is JvmPClass<*> -> pClass.kClass.java

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
                try {
                    Class.forName(className)
                } catch (e: ClassNotFoundException) {
                    throw IllegalArgumentException("Cannot find Java class for: $className", e)
                }
            }
        }
    }

    override val superclass: PClass<*>? by lazy { kClass.java.superclass?.let { Portrait.of(it) } }
    override val interfaces: List<PClass<*>> by lazy { kClass.java.interfaces.map { Portrait.of(it) } }
    override val simpleName: String = kClass.simpleName ?: "<anonymous>"
    override val qualifiedName: String = kClass.java.name
    override val isAbstract: Boolean = kClass.isAbstract
    override val isSealed: Boolean = kClass.isSealed
    override val isData: Boolean = kClass.isData
    override val isCompanion: Boolean = kClass.isCompanion
    override val objectInstance: T? = kClass.objectInstance

    override fun createInstance(vararg args: Any?): T {
        return if (args.isEmpty()) {
            // No arguments - use default constructor
            kClass.java.getDeclaredConstructor().newInstance()
        } else {
            // Find constructor that matches the argument types
            val argTypes = args.map { arg ->
                when (arg) {
                    null -> Any::class.java // For null arguments, we'll need more sophisticated matching
                    else -> arg::class.java
                }
            }.toTypedArray()

            // Try to find exact match first
            val exactConstructor = try {
                kClass.java.getDeclaredConstructor(*argTypes)
            } catch (e: NoSuchMethodException) {
                null
            }

            if (exactConstructor != null) {
                exactConstructor.newInstance(*args)
            } else {
                // Try to find compatible constructor
                val compatibleConstructor = kClass.java.declaredConstructors.find { constructor ->
                    constructor.parameterCount == args.size &&
                            constructor.parameterTypes.zip(args).all { (paramType, arg) ->
                                arg == null || paramType.isAssignableFrom(arg::class.java)
                            }
                }

                if (compatibleConstructor != null) {
                    @Suppress("UNCHECKED_CAST")
                    compatibleConstructor.newInstance(*args) as T
                } else {
                    throw IllegalArgumentException("No constructor found for arguments: ${args.map { it?.let { it::class.simpleName } ?: "null" }}")
                }
            }
        }
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        val otherJavaClass = pClassToJavaClass(other)
        if (kClass.java.isAssignableFrom(otherJavaClass)) {
            return true
        }

        val wrapperPrimitive = BoxedPrimitives.unboxing[qualifiedName]
        return wrapperPrimitive != null &&
                other.isPrimitive &&
                other.qualifiedName == wrapperPrimitive
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        val otherJavaClass = pClassToJavaClass(other)
        return otherJavaClass != kClass.java && otherJavaClass.isAssignableFrom(kClass.java)
    }

    override val annotations: List<PAnnotation> by lazy {
        kClass.annotations.map { JvmPAnnotation(it) }
    }

    override fun getAnnotation(annotationClass: PClass<*>): PAnnotation? {
        val targetClass = pClassToJavaClass(annotationClass).kotlin
        return kClass.annotations.find { it.annotationClass == targetClass }?.let { JvmPAnnotation(it) }
    }

    override fun hasAnnotation(annotationClass: PClass<*>): Boolean {
        val targetClass = pClassToJavaClass(annotationClass).kotlin
        return kClass.annotations.any { it.annotationClass == targetClass }
    }

    override val constructors: List<PConstructor<T>> by lazy {
        kClass.java.declaredConstructors.map {
            @Suppress("UNCHECKED_CAST")
            JvmPConstructor(it as java.lang.reflect.Constructor<T>)
        }
    }

    override fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>? {
        return try {
            val javaTypes = parameterTypes.map { pClassToJavaClass(it) }.toTypedArray()
            val constructor = kClass.java.getConstructor(*javaTypes)
            JvmPConstructor(constructor)
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    override val methods: List<PMethod> by lazy {
        kClass.java.declaredMethods.map { JvmPMethod(it) }
    }

    override fun getMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? =
        try {
            val javaTypes = parameterTypes.map { pClassToJavaClass(it) }.toTypedArray()
            JvmPMethod(kClass.java.getMethod(name, *javaTypes))
        } catch (e: NoSuchMethodException) {
            null
        }

    override val fields: List<PField> by lazy {
        kClass.java.declaredFields.map { JvmPField(it) }
    }

    override fun getField(name: String): PField? =
        try {
            JvmPField(kClass.java.getDeclaredField(name))
        } catch (e: NoSuchFieldException) {
            null
        }

    override fun createProxy(handler: ProxyHandler<T>): T {
        // Validate that the class is suitable for proxying
        if (!kClass.java.isInterface) {
            throw IllegalArgumentException("Cannot create proxy for non-interface type: ${kClass.java.name}")
        }

        try {
            // Create JDK dynamic proxy
            val proxyInstance = Proxy.newProxyInstance(
                kClass.java.classLoader,
                arrayOf(kClass.java),
                JvmProxyInvocationHandler(handler)
            )

            @Suppress("UNCHECKED_CAST")
            return proxyInstance as T
        } catch (e: Exception) {
            throw ProxyCreationException(
                "Failed to create proxy for interface ${kClass.java.name}", e
            )
        }
    }

}

/**
 * InvocationHandler that bridges JDK dynamic proxy calls to Portrait ProxyHandler.
 */
private class JvmProxyInvocationHandler<T : Any>(
    private val handler: ProxyHandler<T>
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        // Wrap the Java Method in a PMethod
        val pMethod = JvmPMethod(method)

        // Delegate to the ProxyHandler
        @Suppress("UNCHECKED_CAST")
        return handler.invoke(proxy as T, pMethod, args)
    }
}
