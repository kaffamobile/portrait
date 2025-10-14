package kaffa.portrait

import kotlin.reflect.KClass

/**
 * Marks a class as requiring reflection capabilities at runtime.
 *
 * This annotation can be applied to:
 * - Interfaces
 * - Abstract classes
 * - Enums
 * - Concrete classes
 *
 * When applied, the annotated class and its members become accessible
 * through the Portrait reflection API.
 *
 * @param including Array of [Includes] options to specify which related
 *                  types should also be marked as reflective. Defaults
 *                  to empty (only the annotated type itself). Use
 *                  [Includes.PUBLIC_API] to include every type that appears
 *                  in the public API surface of the annotated class, and
 *                  combine with [Includes.PUBLIC_API_SUPERTYPES] and/or
 *                  [Includes.PUBLIC_API_SUBTYPES] for directional transitive
 *                  expansion.
 *
 * Example:
 * ```kotlin
 * @Reflective(including = [ALL_SUBTYPES, DIRECT_SUPERTYPES])
 * interface MyService
 * ```
 *
 * @see ProxyTarget
 * @see Includes
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reflective(
    val including: Array<Includes> = []
) {
    /**
     * Allows opt-in of other classes for Reflective capabilities without
     * modifying those classes directly.
     *
     * This annotation is useful when you need to enable reflection for
     * classes that you cannot modify (e.g., third-party libraries,
     * framework classes, or legacy code).
     *
     * @param classes Array of classes that should be treated as if they
     *                were annotated with @Reflective.
     * @param including Array of [Includes] options to specify which related
     *                  types should also be marked as reflective (for example,
     *                  [Includes.PUBLIC_API] to pull in the full public surface
     *                  and [Includes.PUBLIC_API_SUPERTYPES]/[Includes.PUBLIC_API_SUBTYPES]
     *                  for directional transitive expansion).
     *
     * Example:
     * ```kotlin
     * @Reflective.Include(
     *     classes = [ThirdPartyClass::class, LegacyService::class],
     *     including = [DIRECT_SUBTYPES]
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
