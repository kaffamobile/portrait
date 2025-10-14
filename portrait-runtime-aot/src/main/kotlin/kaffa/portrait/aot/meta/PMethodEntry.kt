package kaffa.portrait.aot.meta

data class PMethodEntry(
    val name: String,
    val parameterTypeNames: List<String>,
    val returnTypeName: String,
    val declaringClassName: String,
    val isPublic: Boolean,
    val isPrivate: Boolean,
    val isProtected: Boolean,
    val isStatic: Boolean,
    val isFinal: Boolean,
    val isAbstract: Boolean,
    val annotations: List<PAnnotationEntry>,
    val parameterAnnotations: List<List<PAnnotationEntry>>
)