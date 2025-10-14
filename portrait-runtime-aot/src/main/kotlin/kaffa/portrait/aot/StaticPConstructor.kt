package kaffa.portrait.aot

import kaffa.portrait.PAnnotation
import kaffa.portrait.PClass
import kaffa.portrait.PConstructor
import kaffa.portrait.Portrait
import kaffa.portrait.aot.meta.PConstructorEntry

/**
 * AOT implementation of PConstructor that uses precomputed metadata.
 */
class StaticPConstructor<T : Any>(
    private val constructorEntry: PConstructorEntry,
    private val index: Int,
    private val declaringPClass: PClass<T>,
    private val staticPortrait: StaticPortrait<T>
) : PConstructor<T>() {

    override val declaringClass: PClass<T> get() = declaringPClass

    override val parameterTypes: List<PClass<*>> by lazy {
        constructorEntry.parameterTypeNames.map { typeName ->
            Portrait.forNameOrUnresolved(typeName)
        }
    }

    override val annotations: List<PAnnotation> by lazy {
        constructorEntry.annotations.map { StaticPAnnotation(it) }
    }

    override val isPublic: Boolean get() = constructorEntry.isPublic
    override val isPrivate: Boolean get() = constructorEntry.isPrivate
    override val isProtected: Boolean get() = constructorEntry.isProtected

    override fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation? =
        annotations.find { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean =
        annotations.any { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override fun call(vararg args: Any?): T {
        return staticPortrait.invokeConstructor(index, args)
    }

    override fun callBy(args: List<Any?>): T {
        return staticPortrait.invokeConstructor(index, args.toTypedArray())
    }

    override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
        if (argumentTypes.size != parameterTypes.size) return false

        return parameterTypes.zip(argumentTypes.toList()).all { (paramType, argType) ->
            paramType.isAssignableFrom(argType)
        }
    }
}