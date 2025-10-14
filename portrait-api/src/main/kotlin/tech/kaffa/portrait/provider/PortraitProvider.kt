package tech.kaffa.portrait.provider

import tech.kaffa.portrait.PClass

/**
 * Service provider interface for platform-specific Portrait implementations.
 *
 * PortraitProvider allows different runtime environments (JVM, AOT, etc.) to
 * provide their own reflection implementations while maintaining a unified API.
 *
 * Providers are discovered automatically via Java's ServiceLoader mechanism.
 * When multiple providers are available, they are tried in priority order
 * (highest priority first) until one successfully resolves the requested class.
 *
 * To implement a custom provider:
 * 1. Implement this interface
 * 2. Create a META-INF/services/tech.kaffa.portrait.provider.PortraitProvider file
 * 3. List your implementation class name in that file
 *
 * Example provider registration:
 * ```
 * // In META-INF/services/tech.kaffa.portrait.provider.PortraitProvider
 * com.example.MyCustomPortraitProvider
 * ```
 */
interface PortraitProvider {

    /**
     * Returns the priority of this provider.
     *
     * Higher priority providers are tried first when resolving class names.
     * This allows more specialized providers to override general-purpose ones.
     *
     * Recommended priority ranges:
     * - 0-99: Low priority (fallback implementations)
     * - 100-199: Standard priority (default JVM implementation uses 100)
     * - 200-299: High priority (specialized optimizations)
     * - 300+: Maximum priority (testing, debugging, overrides)
     *
     * @return Priority value, higher numbers = higher priority
     */
    fun priority(): Int

    /**
     * Attempts to resolve a class name into a PClass instance.
     *
     * This method should return null if the provider cannot handle the given
     * class name, allowing other providers to attempt resolution.
     *
     * Implementations should:
     * - Return null (not throw) if the class cannot be found
     * - Return null (not throw) if the class exists but this provider cannot handle it
     * - Only throw exceptions for unexpected errors (not normal lookup failures)
     *
     * @param className The fully qualified class name to resolve
     * @return A PClass instance if successful, null if this provider cannot handle it
     */
    fun <T : Any> forName(className: String): PClass<T>?
}