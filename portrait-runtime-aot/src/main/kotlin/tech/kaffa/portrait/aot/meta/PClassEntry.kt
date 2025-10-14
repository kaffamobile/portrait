package tech.kaffa.portrait.aot.meta

data class PClassEntry(
    val simpleName: String,
    val qualifiedName: String?,
    val isAbstract: Boolean,
    val isSealed: Boolean,
    val isData: Boolean,
    val isCompanion: Boolean,
    val isObject: Boolean,
    val javaClassName: String,
    val superclassName: String?,
    val interfaceNames: List<String>,
    val annotations: List<PAnnotationEntry>,
    val constructors: List<PConstructorEntry>,
    val declaredMethods: List<PMethodEntry>,
    val declaredFields: List<PFieldEntry>,
    val proxyMethods: List<PMethodEntry>
)