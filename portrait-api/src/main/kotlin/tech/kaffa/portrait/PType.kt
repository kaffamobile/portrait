package tech.kaffa.portrait

/**
 * Platform-agnostic representation of a generic type used by Portrait.
 *
 * Implementations mirror the hierarchy of [java.lang.reflect.Type] without exposing it directly,
 * allowing both reflection-based and ahead-of-time runtimes to share metadata about generic
 * signatures.
 */
sealed interface PType

/**
 * Represents a parameterized type such as `List<String>`.
 */
data class PParameterizedType(
    val rawType: PClass<*>,
    val ownerType: PType?,
    val arguments: List<PType>
) : PType

/**
 * Represents a generic type variable such as `T extends Any`.
 */
data class PTypeVariable(
    val name: String,
    val bounds: List<PType>
) : PType

/**
 * Represents a wildcard type such as `? extends Number` or `? super String`.
 */
data class PWildcardType(
    val upperBounds: List<PType>,
    val lowerBounds: List<PType>
) : PType

/**
 * Represents an array whose component type is itself generic (for example `T[]`).
 */
data class PGenericArrayType(
    val componentType: PType
) : PType

/**
 * Returns a human-readable representation similar to [java.lang.reflect.Type.getTypeName].
 */
fun PType.typeName(): String = when (this) {
    is PClass<*> -> qualifiedName
    is PGenericArrayType -> "${componentType.typeName()}[]"
    is PParameterizedType -> buildString {
        if (ownerType != null) {
            append(ownerType.typeName())
            append(".")
            append(rawType.simpleName)
        } else {
            append(rawType.qualifiedName)
        }
        if (arguments.isNotEmpty()) {
            append(arguments.joinToString(prefix = "<", postfix = ">", transform = PType::typeName))
        }
    }
    is PTypeVariable -> buildString {
        append(name)
        val meaningfulBounds = bounds.filterNot { bound ->
            bound is PClass<*> && bound.qualifiedName == "java.lang.Object"
        }
        if (meaningfulBounds.isNotEmpty()) {
            append(meaningfulBounds.joinToString(prefix = " extends ", transform = PType::typeName))
        }
    }
    is PWildcardType -> when {
        lowerBounds.isNotEmpty() -> lowerBounds.joinToString(prefix = "? super ", transform = PType::typeName)
        upperBounds.isEmpty() -> "?"
        upperBounds.size == 1 && upperBounds.first() is PClass<*> &&
            (upperBounds.first() as PClass<*>).qualifiedName == "java.lang.Object" -> "?"
        else -> upperBounds.joinToString(prefix = "? extends ", transform = PType::typeName)
    }
}
