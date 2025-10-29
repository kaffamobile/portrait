package tech.kaffa.portrait.aot

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PClassType
import tech.kaffa.portrait.PGenericArrayType
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.PParameterizedType
import tech.kaffa.portrait.PType
import tech.kaffa.portrait.PTypeVariable
import tech.kaffa.portrait.PWildcardType
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.aot.meta.PMethodEntry
import tech.kaffa.portrait.aot.meta.PClassTypeEntry
import tech.kaffa.portrait.aot.meta.PGenericArrayTypeEntry
import tech.kaffa.portrait.aot.meta.PParameterizedTypeEntry
import tech.kaffa.portrait.aot.meta.PTypeEntry
import tech.kaffa.portrait.aot.meta.PTypeVariableEntry
import tech.kaffa.portrait.aot.meta.PWildcardTypeEntry

/**
 * AOT implementation of PMethod that uses precomputed metadata.
 */
class StaticPMethod(
    private val methodEntry: PMethodEntry,
    private val index: Int,
    private val staticPortrait: StaticPortrait<*>
) : PMethod() {

    override val name: String get() = methodEntry.name

    override val parameterTypes: List<PClass<*>> by lazy {
        methodEntry.parameterTypeNames.map { typeName ->
            Portrait.forNameOrUnresolved(typeName)
        }
    }

    override val parameterCount: Int get() = methodEntry.parameterTypeNames.size

    override val returnType: PClass<*> by lazy {
        Portrait.forNameOrUnresolved(methodEntry.returnTypeName)
    }

    override val genericReturnType: PType by lazy {
        methodEntry.genericReturnType.toPType()
    }

    override val declaringClass: PClass<*> by lazy {
        Portrait.forNameOrUnresolved(methodEntry.declaringClassName)
    }

    override val isStatic: Boolean get() = methodEntry.isStatic
    override val isFinal: Boolean get() = methodEntry.isFinal
    override val isAbstract: Boolean get() = methodEntry.isAbstract

    override val annotations: List<PAnnotation> by lazy {
        methodEntry.annotations.map { StaticPAnnotation(it) }
    }

    override fun getAnnotation(annotationClass: PClass<out Annotation>): PAnnotation? =
        annotations.find { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override fun hasAnnotation(annotationClass: PClass<out Annotation>): Boolean =
        annotations.any { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override fun invoke(instance: Any?, vararg args: Any?): Any? {
        return staticPortrait.invokeMethod(index, instance, args)
    }

    override fun isCallableWith(vararg argumentTypes: PClass<*>): Boolean {
        if (argumentTypes.size != parameterTypes.size) return false

        return parameterTypes.zip(argumentTypes.toList()).all { (paramType, argType) ->
            paramType.isAssignableFrom(argType)
        }
    }

    override val parameterAnnotations: List<List<PAnnotation>> by lazy {
        methodEntry.parameterAnnotations.map { paramAnnotations ->
            paramAnnotations.map { StaticPAnnotation(it) }
        }
    }
}

private fun PTypeEntry.toPType(): PType = when (this) {
    is PClassTypeEntry -> PClassType(Portrait.forNameOrUnresolved(className))
    is PParameterizedTypeEntry -> PParameterizedType(
        rawType = Portrait.forNameOrUnresolved(rawTypeName),
        ownerType = ownerType?.toPType(),
        arguments = arguments.map { it.toPType() }
    )
    is PTypeVariableEntry -> PTypeVariable(
        name = name,
        bounds = bounds.map { it.toPType() }
    )
    is PWildcardTypeEntry -> PWildcardType(
        upperBounds = upperBounds.map { it.toPType() },
        lowerBounds = lowerBounds.map { it.toPType() }
    )
    is PGenericArrayTypeEntry -> PGenericArrayType(componentType.toPType())
}
