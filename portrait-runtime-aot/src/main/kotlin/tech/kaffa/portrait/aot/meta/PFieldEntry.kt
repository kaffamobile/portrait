package tech.kaffa.portrait.aot.meta

data class PFieldEntry(
    val name: String,
    val typeName: String,
    val declaringClassName: String,
    val isPublic: Boolean,
    val isPrivate: Boolean,
    val isProtected: Boolean,
    val isStatic: Boolean,
    val isFinal: Boolean,
    val annotations: List<PAnnotationEntry>
)