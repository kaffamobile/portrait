package tech.kaffa.portrait.aot

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PConstructor
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.aot.meta.PConstructorEntry

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

    override val annotations: List<PAnnotation<*>> by lazy {
        constructorEntry.annotations.map { StaticPAnnotation<Annotation>(it) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation> getAnnotation(annotationClass: PClass<A>): PAnnotation<A>? =
        annotations.find { it.annotationClass == annotationClass } as PAnnotation<A>?

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean =
        annotations.any { it.annotationClass == annotationClass }

    override fun newInstance(vararg args: Any?): T {
        return staticPortrait.invokeConstructor(index, args)
    }

    override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
        if (argumentTypes.size != parameterTypes.size) return false

        return parameterTypes.zip(argumentTypes.toList()).all { (paramType, argType) ->
            paramType.isAssignableFrom(argType)
        }
    }
}
