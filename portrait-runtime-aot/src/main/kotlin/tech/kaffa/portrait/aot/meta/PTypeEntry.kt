package tech.kaffa.portrait.aot.meta

sealed interface PTypeEntry

data class PClassTypeEntry(
    val className: String
) : PTypeEntry

data class PParameterizedTypeEntry(
    val rawTypeName: String,
    val ownerType: PTypeEntry?,
    val arguments: List<PTypeEntry>
) : PTypeEntry

data class PTypeVariableEntry(
    val name: String,
    val bounds: List<PTypeEntry>
) : PTypeEntry

data class PWildcardTypeEntry(
    val upperBounds: List<PTypeEntry>,
    val lowerBounds: List<PTypeEntry>
) : PTypeEntry

data class PGenericArrayTypeEntry(
    val componentType: PTypeEntry
) : PTypeEntry
