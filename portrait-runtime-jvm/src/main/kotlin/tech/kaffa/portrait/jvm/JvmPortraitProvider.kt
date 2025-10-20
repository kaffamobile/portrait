package tech.kaffa.portrait.jvm

import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.provider.PortraitProvider
import java.lang.reflect.Modifier

/**
 * JVM-based implementation of PortraitProvider using standard Java reflection.
 *
 * This provider uses Java's Class.forName() and Kotlin reflection to create
 * Portrait wrappers for classes available on the JVM classpath.
 *
 * Priority: 100 (standard priority for general-purpose JVM reflection)
 *
 * This provider will successfully resolve any class that:
 * - Is available on the current classpath
 * - Can be loaded by Class.forName()
 * - Is accessible to the current security context
 *
 * This is typically the default provider for JVM applications when
 * portrait-runtime-jvm is included as a dependency.
 */
class JvmPortraitProvider : PortraitProvider {

    /**
     * Returns priority 100 for standard JVM reflection support.
     *
     * @return 100 (standard priority)
     */
    override fun priority(): Int = 100

    /**
     * Attempts to load a class using standard JVM Class.forName().
     *
     * @param className Fully qualified class name to load
     * @return JvmPClass wrapper if successful, null if class not found
     */
    override fun <T : Any> forName(className: String): PClass<T>? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val javaClass = Class.forName(className) as Class<T>
            if (!isPubliclyAccessible(javaClass)) {
                return null
            }
            JvmPClass(javaClass.kotlin)
        } catch (e: Exception) {
            null
        }
    }

    private fun isPubliclyAccessible(clazz: Class<*>): Boolean {
        if (clazz.canonicalName == null) return false
        if (!Modifier.isPublic(clazz.modifiers)) return false

        var enclosing: Class<*>? = clazz.enclosingClass
        while (enclosing != null) {
            if (!Modifier.isPublic(enclosing.modifiers)) return false
            enclosing = enclosing.enclosingClass
        }
        return true
    }
}
