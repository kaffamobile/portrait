package tech.kaffa.portrait.jvm

import tech.kaffa.portrait.PGenericArrayType
import tech.kaffa.portrait.PParameterizedType
import tech.kaffa.portrait.PType
import tech.kaffa.portrait.PTypeVariable
import tech.kaffa.portrait.PWildcardType
import tech.kaffa.portrait.Portrait
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

internal fun Type.toPortraitType(): PType = when (this) {
    is Class<*> -> Portrait.of(this)
    is ParameterizedType -> PParameterizedType(
        rawType = Portrait.of(rawType as Class<*>),
        ownerType = ownerType?.toPortraitType(),
        arguments = actualTypeArguments.map { it.toPortraitType() }
    )
    is GenericArrayType -> PGenericArrayType(genericComponentType.toPortraitType())
    is TypeVariable<*> -> PTypeVariable(
        name = name,
        bounds = bounds.map { it.toPortraitType() }
    )
    is WildcardType -> PWildcardType(
        upperBounds = upperBounds.map { it.toPortraitType() },
        lowerBounds = lowerBounds.map { it.toPortraitType() }
    )
    else -> throw IllegalArgumentException("Unsupported type implementation: ${this::class.java.name}")
}
