package kaffa.portrait.aot

import kaffa.portrait.PAnnotation
import kaffa.portrait.PClass
import kaffa.portrait.Portrait
import kaffa.portrait.aot.meta.PAnnotationEntry

/**
 * AOT implementation of PAnnotation that uses precomputed metadata.
 */
class StaticPAnnotation(
    private val annotationEntry: PAnnotationEntry
) : PAnnotation() {

    override val annotationClass: PClass<out Annotation> by lazy {
        @Suppress("UNCHECKED_CAST")
        Portrait.forNameOrUnresolved(annotationEntry.annotationClassName) as PClass<out Annotation>
    }

    override val simpleName: String get() = annotationEntry.simpleName
    override val qualifiedName: String? get() = annotationEntry.qualifiedName

    override fun getValue(propertyName: String): Any? {
        return annotationEntry.properties[propertyName]
    }

}