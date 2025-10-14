package tech.kaffa.portrait.aot.meta

data class PConstructorEntry(
    val declaringClassName: String,
    val parameterTypeNames: List<String>,
    val annotations: List<PAnnotationEntry>,
    val isPublic: Boolean,
    val isPrivate: Boolean,
    val isProtected: Boolean
)