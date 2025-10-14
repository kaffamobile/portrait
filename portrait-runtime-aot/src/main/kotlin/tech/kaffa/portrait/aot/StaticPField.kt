package tech.kaffa.portrait.aot

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PField
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.aot.meta.PFieldEntry

/**
 * AOT implementation of PField that uses precomputed metadata.
 */
class StaticPField(
    private val fieldEntry: PFieldEntry,
    private val index: Int,
    private val staticPortrait: StaticPortrait<*>
) : PField() {

    override val name: String get() = fieldEntry.name

    override val type: PClass<*> by lazy {
        Portrait.forNameOrUnresolved(fieldEntry.typeName)
    }

    override val declaringClass: PClass<*> by lazy {
        Portrait.forNameOrUnresolved(fieldEntry.declaringClassName)
    }

    override val isPublic: Boolean get() = fieldEntry.isPublic
    override val isPrivate: Boolean get() = fieldEntry.isPrivate
    override val isProtected: Boolean get() = fieldEntry.isProtected
    override val isStatic: Boolean get() = fieldEntry.isStatic
    override val isFinal: Boolean get() = fieldEntry.isFinal

    override val annotations: List<PAnnotation> by lazy {
        fieldEntry.annotations.map { StaticPAnnotation(it) }
    }

    override fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation? =
        annotations.find { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean =
        annotations.any { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override fun get(instance: Any?): Any? {
        return staticPortrait.getFieldValue(index, instance)
    }

    override fun set(instance: Any?, value: Any?) {
        staticPortrait.setFieldValue(index, instance, value)
    }
}