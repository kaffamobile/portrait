package tech.kaffa.portrait

import tech.kaffa.portrait.internal.UnresolvedPClass
import tech.kaffa.portrait.provider.PortraitProvider
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

    private const val LOADING_SENTINEL_NAME = "<portrait:loading>"
    private const val NOT_FOUND_SENTINEL_NAME = "<portrait:not-found>"

    /**
     * Lazily-loaded list of available providers, sorted by priority (highest first).
     *
     * Providers are discovered via Java's ServiceLoader mechanism and sorted by their
     * priority() method. Higher priority providers are tried first.
     */
    private val providers: List<PortraitProvider> by lazy {
        ServiceLoader.load(PortraitProvider::class.java).toList()
            .sortedByDescending { it.priority() }
            .takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("No PortraitProvider implementation found on classpath")
    }

    /**
     * Sentinel value to mark classes that are currently being loaded to detect cycles.
     */
    private val loadingMarker: PClass<*> = UnresolvedPClass<Any>(LOADING_SENTINEL_NAME)

    /**
     * Sentinel value to record classes that failed to resolve to avoid re-running provider lookups.
     */
    private val notFoundMarker: PClass<*> = UnresolvedPClass<Any>(NOT_FOUND_SENTINEL_NAME)

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
        return try {
            load(clazz.name)
        } catch (e: PortraitNotFoundException) {
            throw PortraitNotFoundException(
                "Cannot create portrait for known Java class: ${clazz.name}",
                e
            )
        }
    }

    /**
     * Attempts to create a [PClass] from a Java [Class], returning an unresolved placeholder if the
     * type cannot be provided by the available [PortraitProvider]s.
     *
     * Use this variant when you need to continue operating even if reflection data is missing.
     *
     * @param clazz Java class to wrap
     * @return A resolved [PClass] or an [UnresolvedPClass] sentinel
     */
    @JvmStatic
    fun <T : Any> ofOrUnresolved(clazz: Class<T>): PClass<T> {
        return try {
            load(clazz.name)
        } catch (_: PortraitNotFoundException) {
            UnresolvedPClass(clazz.name)
        }
    }

    /**
     * Attempts to create a [PClass] from a Java [Class], returning `null` when no provider can
     * handle the class. Unlike [of], this does not throw.
     *
     * @param clazz Java class to wrap
     * @return A resolved [PClass] or `null` when unavailable
     */
    @JvmStatic
    fun <T : Any> ofOrNull(clazz: Class<T>): PClass<T>? {
        return try {
            load(clazz.name)
        } catch (_: PortraitNotFoundException) {
            null
        }
    }

    @JvmStatic
    fun <T : Any> of(clazz: KClass<T>): PClass<T> {
        val className = clazz.java.name
        return try {
            load(className)
        } catch (e: PortraitNotFoundException) {
            throw PortraitNotFoundException(
                "Cannot create portrait for known Kotlin class: $className",
                e
            )
        }
    }

    /**
     * Attempts to create a [PClass] from a Kotlin [KClass], returning an unresolved placeholder if
     * the type cannot be provided by the available [PortraitProvider]s.
     *
     * @param clazz Kotlin class to wrap
     * @return A resolved [PClass] or an [UnresolvedPClass] sentinel
     */
    @JvmStatic
    fun <T : Any> ofOrUnresolved(clazz: KClass<T>): PClass<T> {
        return try {
            load(clazz.java.name)
        } catch (_: PortraitNotFoundException) {
            UnresolvedPClass(clazz.java.name)
        }
    }

    /**
     * Attempts to create a [PClass] from a Kotlin [KClass], returning `null` when no provider can
     * handle the class. Unlike [of], this does not throw.
     *
     * @param clazz Kotlin class to wrap
     * @return A resolved [PClass] or `null` when unavailable
     */
    @JvmStatic
    fun <T : Any> ofOrNull(clazz: KClass<T>): PClass<T>? {
        return try {
            load(clazz.java.name)
        } catch (_: PortraitNotFoundException) {
            null
        }
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
        val className = instance.javaClass.name
        return try {
            load(className)
        } catch (e: PortraitNotFoundException) {
            throw PortraitNotFoundException(
                "Cannot create portrait for instance of type: $className",
                e
            )
        }
    }

    /**
     * Attempts to create a [PClass] from an instance, returning an unresolved placeholder if the
     * runtime type cannot be provided by the available [PortraitProvider]s.
     *
     * @param instance Instance whose runtime type should be wrapped
     * @return A resolved [PClass] or an [UnresolvedPClass] sentinel
     */
    @JvmStatic
    fun <T : Any> fromOrUnresolved(instance: T): PClass<T> {
        return try {
            load(instance.javaClass.name)
        } catch (_: PortraitNotFoundException) {
            UnresolvedPClass(instance.javaClass.name)
        }
    }

    /**
     * Attempts to create a [PClass] from an instance, returning `null` when no provider can handle
     * the runtime type.
     *
     * @param instance Instance whose runtime type should be wrapped
     * @return A resolved [PClass] or `null` when unavailable
     */
    @JvmStatic
    fun <T : Any> fromOrNull(instance: T): PClass<T>? {
        return try {
            load(instance.javaClass.name)
        } catch (_: PortraitNotFoundException) {
            null
        }
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
        return try {
            load<Any>(className)
        } catch (e: PortraitNotFoundException) {
            throw PortraitNotFoundException(
                "Cannot find class by name: $className",
                e
            )
        }
    }

    /**
     * Attempts to create a [PClass] by class name, returning `null` if no provider can resolve it.
     * Unlike [forName], this does not throw when the class cannot be found.
     *
     * @param className Fully qualified class name to resolve
     * @return A resolved [PClass] or `null` when unavailable
     */
    @JvmStatic
    fun forNameOrNull(className: String): PClass<*>? {
        return try {
            load<Any>(className)
        } catch (_: PortraitNotFoundException) {
            null
        }
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
        return try {
            load<Any>(className)
        } catch (_: PortraitNotFoundException) {
            UnresolvedPClass<Any>(className)
        }
    }

    /**
     * Internal method that tries each provider in priority order to resolve a class name.
     * Uses caching to prevent circular dependencies and improve performance.
     *
     * @param className The class name to resolve
     * @return A PClass if any provider can handle it
     * @throws RuntimeException if no providers are available or circular dependency detected
     */
    private fun <T : Any> load(className: String): PClass<T> {
        // Check cache first
        val cached = cache[className]
        if (cached != null) {
            when {
                cached === loadingMarker -> {
                    throw IllegalStateException("Circular dependency detected while loading class: $className")
                }
                cached === notFoundMarker -> {
                    throw PortraitNotFoundException("No Portrait available for $className")
                }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    return cached as PClass<T>
                }
            }
        }

        // Mark as loading to detect cycles
        cache[className] = loadingMarker

        try {
            if (providers.isEmpty()) {
                throw IllegalStateException("No PortraitProvider implementation found on classpath")
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
            cache[className] = notFoundMarker
            throw PortraitNotFoundException("No Portrait available for $className")
        } catch (e: Exception) {
            if (e is PortraitNotFoundException) {
                cache[className] = notFoundMarker
            } else {
                cache.remove(className, loadingMarker)
                e.printStackTrace()
            }
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
        return try {
            load<Any>(primitiveName)
        } catch (_: PortraitNotFoundException) {
            null
        }
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
    /**
     * Returns the Portrait metadata wrapper for the primitive `boolean` type.
     *
     * @return The cached `PClass` representing `boolean`
     */
    @JvmStatic
    fun booleanClass(): PClass<Boolean> = load("boolean")

    /**
     * Returns the Portrait metadata wrapper for the primitive `byte` type.
     *
     * @return The cached `PClass` representing `byte`
     */
    @JvmStatic
    fun byteClass(): PClass<Byte> = load("byte")

    /**
     * Returns the Portrait metadata wrapper for the primitive `char` type.
     *
     * @return The cached `PClass` representing `char`
     */
    @JvmStatic
    fun charClass(): PClass<Char> = load("char")

    /**
     * Returns the Portrait metadata wrapper for the primitive `short` type.
     *
     * @return The cached `PClass` representing `short`
     */
    @JvmStatic
    fun shortClass(): PClass<Short> = load("short")

    /**
     * Returns the Portrait metadata wrapper for the primitive `int` type.
     *
     * @return The cached `PClass` representing `int`
     */
    @JvmStatic
    fun intClass(): PClass<Int> = load("int")

    /**
     * Returns the Portrait metadata wrapper for the primitive `long` type.
     *
     * @return The cached `PClass` representing `long`
     */
    @JvmStatic
    fun longClass(): PClass<Long> = load("long")

    /**
     * Returns the Portrait metadata wrapper for the primitive `float` type.
     *
     * @return The cached `PClass` representing `float`
     */
    @JvmStatic
    fun floatClass(): PClass<Float> = load("float")

    /**
     * Returns the Portrait metadata wrapper for the primitive `double` type.
     *
     * @return The cached `PClass` representing `double`
     */
    @JvmStatic
    fun doubleClass(): PClass<Double> = load("double")

    /**
     * Returns the Portrait metadata wrapper for the `void` pseudo-type.
     *
     * @return The cached `PClass` representing `void`
     */
    @JvmStatic
    fun voidClass(): PClass<Void> = load("void")

}
