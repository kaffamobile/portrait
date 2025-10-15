package tech.kaffa.portrait

/**
 * Enumeration defining the inclusion options for related types
 * when processing annotations in Portrait.
 */
enum class Includes {

    /**
     * Includes only the **immediate subclasses or subinterfaces** of a given type.
     *
     * Use this when only direct descendants are relevant,
     * avoiding traversal into deeper subtype hierarchies.
     */
    DIRECT_SUBTYPES,

    /**
     * Includes only the **immediate superclasses or superinterfaces** of a given type.
     *
     * Useful for retrieving direct parent types without
     * walking the entire supertype chain.
     */
    DIRECT_SUPERTYPES,

    /**
     * Includes **all transitive subtypes** of a given type.
     *
     * This recursively traverses the full subtype hierarchy,
     * making all descendants (at any depth) available for processing.
     */
    ALL_SUBTYPES,

    /**
     * Includes **all transitive supertypes** of a given type.
     *
     * This recursively collects all ancestor classes and interfaces,
     * ensuring complete access to inherited metadata.
     */
    ALL_SUPERTYPES,

    /**
     * Includes all **types referenced by the public API** of a given type.
     *
     * Traverses:
     * - Public method signatures (return types and parameters)
     * - Public constructors
     * - Public fields
     *
     * Ensures that all types reachable through the public API
     * are included for Portrait code generation.
     */
    PUBLIC_API,

    /**
     * Includes all **supertypes** of the types discovered via [PUBLIC_API].
     *
     * Use together with [PUBLIC_API] when superclass or interface metadata
     * is required for accurate model generation.
     */
    PUBLIC_API_SUPERTYPES,

    /**
     * Includes all **subtypes** of the types discovered via [PUBLIC_API].
     *
     * Use together with [PUBLIC_API] when subclass or implementation
     * details are needed to fully capture type relationships.
     */
    PUBLIC_API_SUBTYPES
}
