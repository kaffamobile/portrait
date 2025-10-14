package kaffa.portrait

/**
 * Defines which related types should be included when processing an annotation.
 *
 * These options allow fine-grained control over the type hierarchy:
 * - DIRECT_SUBTYPES: Only immediate subclasses/subinterfaces
 * - DIRECT_SUPERTYPES: Only immediate superclasses/superinterfaces
 * - ALL_SUBTYPES: Entire subtype hierarchy (transitive)
 * - ALL_SUPERTYPES: Entire supertype hierarchy (transitive)
 * - PUBLIC_API: All public API surface reachable from the type (public methods,
 *   constructors, and fields along with the types they reference)
 * - PUBLIC_API_SUPERTYPES: PUBLIC_API plus all supertypes of every discovered type
 * - PUBLIC_API_SUBTYPES: PUBLIC_API plus all subtypes of every discovered type
 *
 * Multiple values can be combined, e.g., [ALL_SUBTYPES, DIRECT_SUPERTYPES]
 */
enum class Includes {
    DIRECT_SUBTYPES,
    DIRECT_SUPERTYPES,
    ALL_SUBTYPES,
    ALL_SUPERTYPES,
    /**
     * Includes all classes referenced from the public API of a type.
     *
     * This traverses the signatures of public methods (return types and parameters),
     * public constructors, and public fields to ensure the entire public surface
     * is available to Portrait code generation.
     */
    PUBLIC_API,
    /**
     * Includes all supertypes for the types discovered via [PUBLIC_API].
     *
     * Use together with [PUBLIC_API] when the code generator needs metadata for
     * superclasses or super-interfaces referenced by the public surface.
     */
    PUBLIC_API_SUPERTYPES,
    /**
     * Includes all subtypes for the types discovered via [PUBLIC_API].
     *
     * Use together with [PUBLIC_API] when the code generator needs metadata for
     * subclasses or implementing types reachable from the public surface.
     */
    PUBLIC_API_SUBTYPES
}
