package kaffa.portrait.jvm

import kaffa.portrait.PAnnotation
import kaffa.portrait.PClass
import kaffa.portrait.Portrait

/**
 * JVM implementation of PAnnotation using Java reflection.
 *
 * This class wraps a Java Annotation and provides Portrait API access to its
 * properties using standard JVM reflection under the hood.
 *
 * Internal implementation detail - users should access this through the
 * Portrait API rather than instantiating directly.
 *
 * @param annotation The Java Annotation to wrap
 */
internal class JvmPAnnotation(private val annotation: Annotation) : PAnnotation() {

    override val annotationClass: PClass<out Annotation> by lazy {
        Portrait.of(annotation.annotationClass.java)
    }
    override val simpleName: String by lazy {
        annotationClass.simpleName
    }
    override val qualifiedName: String? by lazy {
        annotationClass.qualifiedName
    }

    override fun getValue(propertyName: String): Any? {
        return try {
            val method = annotation.javaClass.getMethod(propertyName)
            method.invoke(annotation)
        } catch (e: Exception) {
            null
        }
    }

    override fun equals(other: Any?): Boolean =
        other is JvmPAnnotation && annotation == other.annotation

    override fun hashCode(): Int = annotation.hashCode()
}