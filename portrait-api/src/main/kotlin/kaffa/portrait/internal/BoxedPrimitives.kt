package kaffa.portrait.internal

/**
 * Shared constants for well-known JVM types handled specially by Portrait.
 *
 * These maps are referenced by multiple runtime modules to maintain consistent
 * primitive ↔ boxed relationships without duplicating literals.
 */
object BoxedPrimitives {
    /** Names of primitive types keyed to their boxed counterparts. */
    val boxing: Map<String, String> = mapOf(
        "boolean" to "java.lang.Boolean",
        "byte" to "java.lang.Byte",
        "char" to "java.lang.Character",
        "short" to "java.lang.Short",
        "int" to "java.lang.Integer",
        "long" to "java.lang.Long",
        "float" to "java.lang.Float",
        "double" to "java.lang.Double",
        "void" to "java.lang.Void"
    )

    /** Inverse lookup for boxed types keyed to their primitive counterparts. */
    val unboxing: Map<String, String> = boxing.entries.associate { (primitive, wrapper) -> wrapper to primitive }
}