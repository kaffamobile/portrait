package kaffa.portrait

import kotlin.reflect.KClass

/**
 * Marks an interface as a target for proxy generation.
 *
 * This annotation can only be applied to interfaces and enables
 * runtime proxy creation for the annotated interface.
 *
 * Proxies are useful for:
 * - Dynamic implementations
 * - Interception and decoration
 * - Mock object creation
 * - Cross-cutting concerns (logging, security, etc.)
 *
 * @param including Array of [Includes] options to specify which related
 *                  types should also be marked as proxy targets. Defaults
 *                  to empty (only the annotated type itself). Use
 *                  [Includes.PUBLIC_API] when proxies should reference the
 *                  entire public surface of the interface, and combine with
 *                  [Includes.PUBLIC_API_SUPERTYPES]/[Includes.PUBLIC_API_SUBTYPES]
 *                  for directional transitive expansion.
 *
 * Example:
 * ```kotlin
 * @ProxyTarget(including = [ALL_SUBTYPES])
 * interface MyApi
 * ```
 *
 * @see Reflective
 * @see Includes
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProxyTarget(
    val including: Array<Includes> = []
) {
    /**
     * Allows opt-in of other interfaces for ProxyTarget capabilities without
     * modifying those interfaces directly.
     *
     * This annotation is useful when you need to enable proxy generation for
     * interfaces that you cannot modify (e.g., third-party libraries,
     * framework interfaces, or legacy code).
     *
     * @param classes Array of interface classes that should be treated as if
     *                they were annotated with @ProxyTarget.
     * @param including Array of [Includes] options to specify which related
     *                  types should also be marked as proxy targets (for example,
     *                  [Includes.PUBLIC_API] to bring in all public signatures and
     *                  [Includes.PUBLIC_API_SUPERTYPES]/[Includes.PUBLIC_API_SUBTYPES]
     *                  for directional transitive expansion).
     *
     * Example:
     * ```kotlin
     * @ProxyTarget.Include(
     *     classes = [ExternalInterface::class],
     *     including = [ALL_SUBTYPES]
     * )
     * class MyConfiguration
     * ```
     */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    @Repeatable
    annotation class Include(
        val classes: Array<KClass<*>>,
        val including: Array<Includes> = []
    )
}
