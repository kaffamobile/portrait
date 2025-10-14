package kaffa.portrait.codegen.utils

import kaffa.portrait.aot.meta.PAnnotationEntry
import kaffa.portrait.aot.meta.PConstructorEntry
import kaffa.portrait.aot.meta.PFieldEntry
import kaffa.portrait.aot.meta.PMethodEntry
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.method.ParameterDescription
import net.bytebuddy.description.type.TypeDescription
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("kaffa.portrait.codegen.utils.MetadataUtils")

fun FieldDescription.toPFieldEntry(declaringClassName: String): PFieldEntry {
    return PFieldEntry(
        name = name,
        typeName = type.typeName,
        declaringClassName = declaringClassName,
        isPublic = isPublic,
        isPrivate = isPrivate,
        isProtected = isProtected,
        isStatic = isStatic,
        isFinal = isFinal,
        annotations = toAnnotationEntries()
    )
}

fun MethodDescription.toPMethodEntry(declaringClassName: String): PMethodEntry {
    return PMethodEntry(
        name = name,
        parameterTypeNames = parameterTypeNames(),
        returnTypeName = returnTypeName(),
        declaringClassName = declaringClassName,
        isPublic = isPublic,
        isPrivate = isPrivate,
        isProtected = isProtected,
        isStatic = isStatic,
        isFinal = isFinal,
        isAbstract = isAbstract,
        annotations = toAnnotationEntries(),
        parameterAnnotations = parameters.map { it.toAnnotationEntries() }
    )
}

fun MethodDescription.toPConstructorEntry(declaringClassName: String): PConstructorEntry {
    return PConstructorEntry(
        declaringClassName = declaringClassName,
        parameterTypeNames = parameterTypeNames(),
        annotations = toAnnotationEntries(),
        isPublic = isPublic,
        isPrivate = isPrivate,
        isProtected = isProtected
    )
}

fun TypeDescription.toAnnotationEntries(): List<PAnnotationEntry> =
    declaredAnnotations.map { it.toAnnotationEntry() }

fun FieldDescription.toAnnotationEntries(): List<PAnnotationEntry> =
    declaredAnnotations.map { it.toAnnotationEntry() }

fun MethodDescription.toAnnotationEntries(): List<PAnnotationEntry> =
    declaredAnnotations.map { it.toAnnotationEntry() }

fun ParameterDescription.toAnnotationEntries(): List<PAnnotationEntry> =
    declaredAnnotations.map { it.toAnnotationEntry() }

fun AnnotationDescription.toAnnotationEntry(): PAnnotationEntry {
    return PAnnotationEntry(
        annotationClassName = annotationType.typeName,
        simpleName = annotationType.simpleName,
        qualifiedName = annotationType.typeName.takeIf { it.contains(".") },
        properties = annotationType.declaredMethods
            .filter { !it.isConstructor }
            .associate { method ->
                val value = try {
                    getValue(method.name)?.resolve()
                } catch (e: Exception) {
                    logger.debug(
                        "Failed to resolve annotation property '${method.name}' for annotation '${annotationType.typeName}': ${e.message}",
                        e
                    )
                    null
                }
                method.name to value
            }
            .filterValues { it != null }
    )
}