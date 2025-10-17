package tech.kaffa.portrait

import tech.kaffa.portrait.internal.UnresolvedPClass
import tech.kaffa.portrait.provider.PortraitProvider
import tech.kaffa.portrait.proxy.ProxyHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Primary entry point for the Portrait reflection API.
 *
 * Portrait provides a unified, cross-platform interface for reflection capabilities.
 * It uses a service provider pattern to discover and delegate to platform-specific
 * implementations (JVM, AOT, etc.) while presenting a consistent API.
 *
 * The Portrait object automatically discovers available providers via ServiceLoader
 * and tries them in priority order until one can resolve the requested class.
 *
 * Example usage:
 * ```kotlin
 * // From a Java Class
 * val pClass = Portrait.of(String::class.java)
 *
 * // From a Kotlin KClass
 * val pClass = Portrait.of(String::class)
 *
 * // From an instance
 * val pClass = Portrait.from("hello")
 *
 * // From a class name
 * val pClass = Portrait.forName("java.lang.String")
 * ```
 */
object Portrait {

    /**
     * Cache to prevent circular dependencies and improve performance.
     * Maps class names to their PClass instances.
     */
    private val cache = ConcurrentHashMap<String, PClass<*>>()

    /**
     * Sentinel value to mark classes that are currently being loaded to detect cycles.
     */
    private val loadingMarker = object : PClass<Any>() {
        override val simpleName = "LOADING"
        override val qualifiedName = "LOADING"
        override val isAbstract = false
        override val isSealed = false
        override val isData = false
        override val isCompanion = false
        override val objectInstance = null
        override val superclass = null
        override val interfaces = emptyList<PClass<*>>()
        override fun createInstance(vararg args: Any?) = throw UnsupportedOperationException()
        override fun isAssignableFrom(other: PClass<*>) = false
        override fun isSubclassOf(other: PClass<*>) = false
        override val annotations = emptyList<PAnnotation>()
        override fun getAnnotation(annotationClass: PClass<*>) = null
        override fun hasAnnotation(annotationClass: PClass<*>) = false
        override val constructors = emptyList<PConstructor<Any>>()
        override fun getConstructor(vararg parameterTypes: PClass<*>) = null
        override val methods = emptyList<PMethod>()
        override fun getMethod(name: String, vararg parameterTypes: PClass<*>) = null
        override val fields = emptyList<PField>()
        override fun getField(name: String) = null
        override fun createProxy(handler: ProxyHandler<Any>) = throw UnsupportedOperationException()
    }

    /**
     * Creates a PClass from a Java Class object.
     *
     * @param clazz The Java Class to wrap
     * @return A PClass representing the given class
     * @throws IllegalArgumentException if the class has no canonical name (local/anonymous)
     * @throws PortraitNotFoundException if no provider can handle this class
     */
    @JvmStatic
    fun <T : Any> of(clazz: Class<T>): PClass<T> {
        val className = requireQualifiedClass(clazz)
        return load(className)
            ?: throw PortraitNotFoundException("Cannot create portrait for known Java class: ${clazz.name}")
    }

    @JvmStatic
    fun <T : Any> of(clazz: KClass<T>): PClass<T> {
        val className = requireQualifiedClass(clazz.java)
        return load(className)
            ?: throw PortraitNotFoundException("Cannot create portrait for known Kotlin class: ${clazz.java.name}")
    }


    /**
     * Creates a PClass from an object instance.
     *
     * @param instance The object instance to get the class from
     * @return A PClass representing the instance's runtime type
     * @throws IllegalArgumentException if the runtime type has no canonical name (local/anonymous)
     * @throws PortraitNotFoundException if no provider can handle this class
     */
    @JvmStatic
    fun <T : Any> from(instance: T): PClass<T> {
        val className = requireQualifiedClass(instance.javaClass)
        return load(className)
            ?: throw PortraitNotFoundException("Cannot create portrait for instance of type: ${instance.javaClass.name}")
    }

    /**
     * Creates a PClass by looking up a class name.
     *
     * @param className The fully qualified class name to load
     * @return A PClass representing the named class
     * @throws PortraitNotFoundException if the class cannot be found or loaded
     */
    @JvmStatic
    fun forName(className: String): PClass<*> {
        return load<Any>(className)
            ?: throw PortraitNotFoundException("Cannot find class by name: $className")
    }

    /**
     * Creates a PClass by looking up a class name, returning UnresolvedPClass if not found.
     *
     * This method is intended for internal use by runtime implementations when resolving
     * types from metadata. Unlike forName(), this method never throws exceptions and
     * instead returns an UnresolvedPClass for missing types, allowing the system to
     * continue operating even when some referenced types are unavailable.
     *
     * @param className The fully qualified class name to load
     * @return A PClass representing the named class, or UnresolvedPClass if not found
     */
    @JvmStatic
    fun forNameOrUnresolved(className: String): PClass<*> {
        return load<Any>(className) ?: UnresolvedPClass<Any>(className)
    }

    /**
     * Internal method that tries each provider in priority order to resolve a class name.
     * Uses caching to prevent circular dependencies and improve performance.
     *
     * @param className The class name to resolve
     * @return A PClass if any provider can handle it, null otherwise
     * @throws RuntimeException if no providers are available or circular dependency detected
     */
    private fun <T : Any> load(className: String): PClass<T>? {
        // Check cache first
        val cached = cache[className]
        if (cached != null) {
            if (cached === loadingMarker) {
                throw RuntimeException("Circular dependency detected while loading class: $className")
            }
            @Suppress("UNCHECKED_CAST")
            return cached as PClass<T>
        }

        // Mark as loading to detect cycles
        cache[className] = loadingMarker

        try {
            if (providers.isEmpty()) {
                throw RuntimeException("No PortraitProvider implementation found on classpath")
            }

            for (provider in providers) {
                val result = provider.forName<T>(className)
                if (result != null) {
                    // Cache the successful result
                    cache[className] = result
                    return result
                }
            }

            // No provider could handle this class
            cache.remove(className)
            return null

        } catch (e: Exception) {
            // Remove loading marker on error
            cache.remove(className)
            throw e
        }
    }

    /**
     * Clears the internal cache. Useful for testing or when class definitions change.
     */
    @JvmStatic
    fun clearCache() {
        cache.clear()
    }

    /**
     * Gets a PClass for a primitive type by name.
     *
     * @param primitiveName The name of the primitive type (e.g., "boolean", "int", "double")
     * @return A PClass representing the primitive type, or null if not a valid primitive
     */
    @JvmStatic
    fun forPrimitive(primitiveName: String): PClass<*>? {
        return load<Any>(primitiveName)
    }

    /**
     * Checks if a PClass represents an unresolved type.
     *
     * @param pClass The PClass to check
     * @return true if the PClass is an UnresolvedPClass, false otherwise
     */
    @JvmStatic
    fun isUnresolved(pClass: PClass<*>): Boolean {
        return pClass is UnresolvedPClass<*>
    }

    // Convenience methods for common primitive types
    @JvmStatic
    fun booleanClass(): PClass<Boolean> = load("boolean")!!

    @JvmStatic
    fun byteClass(): PClass<Byte> = load("byte")!!

    @JvmStatic
    fun charClass(): PClass<Char> = load("char")!!

    @JvmStatic
    fun shortClass(): PClass<Short> = load("short")!!

    @JvmStatic
    fun intClass(): PClass<Int> = load("int")!!

    @JvmStatic
    fun longClass(): PClass<Long> = load("long")!!

    @JvmStatic
    fun floatClass(): PClass<Float> = load("float")!!

    @JvmStatic
    fun doubleClass(): PClass<Double> = load("double")!!

    @JvmStatic
    fun voidClass(): PClass<Void> = load("void")!!

    /**
     * Lazily-loaded list of available providers, sorted by priority (highest first).
     *
     * Providers are discovered via Java's ServiceLoader mechanism and sorted by their
     * priority() method. Higher priority providers are tried first.
     */
    private val providers: List<PortraitProvider> by lazy {
        ServiceLoader.load(PortraitProvider::class.java).toList()
            .sortedByDescending { it.priority() }
            .also {
                if (it.isEmpty()) {
                    throw RuntimeException("No PortraitProvider implementation found on classpath")
                }
            }
    }

    /**
     * Validates that the supplied [Class] exposes a canonical name so Portrait can use it as a
     * stable lookup key. Local and anonymous classes don't meet this contract and must be rejected
     * before reaching providers.
     *
     * @throws IllegalArgumentException when [clazz] lacks a canonical name
     */
    private fun requireQualifiedClass(clazz: Class<*>): String {
        val canonicalName = clazz.canonicalName
        if (canonicalName == null) {
            val descriptor = when {
                clazz.isAnonymousClass -> "anonymous"
                clazz.isLocalClass -> "local"
                else -> "synthetic"
            }
            throw IllegalArgumentException(
                "Portrait cannot reflect $descriptor class ${clazz.name}. Local and anonymous classes are unsupported; provide a top-level or member type instead."
            )
        }
        return clazz.name
    }
}
