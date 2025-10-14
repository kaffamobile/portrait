package kaffa.portrait.aot.meta

data class PAnnotationEntry(
    val annotationClassName: String,
    val simpleName: String,
    val qualifiedName: String?,
    val properties: Map<String, Any?>
)