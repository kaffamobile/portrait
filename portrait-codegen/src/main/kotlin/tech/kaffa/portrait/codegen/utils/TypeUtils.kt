package tech.kaffa.portrait.codegen.utils

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDefinition
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.description.type.TypeDescription.Generic
import tech.kaffa.portrait.aot.meta.PClassTypeEntry
import tech.kaffa.portrait.aot.meta.PGenericArrayTypeEntry
import tech.kaffa.portrait.aot.meta.PParameterizedTypeEntry
import tech.kaffa.portrait.aot.meta.PTypeEntry
import tech.kaffa.portrait.aot.meta.PTypeVariableEntry
import tech.kaffa.portrait.aot.meta.PWildcardTypeEntry

fun MethodDescription.parameterTypeNames(): List<String> =
    parameters.asTypeList().map { it.typeName }

fun MethodDescription.returnTypeName(): String =
    returnType.typeName

fun TypeDescription.isObjectClass(): Boolean =
    represents(Any::class.java)

fun TypeDescription.superclassNameOrNull(): String? {
    if (isPrimitive || isInterface || isObjectClass()) return null

    return superClass?.asErasure()?.typeName
}

fun TypeDescription.interfaceNames(): List<String> =
    interfaces.asErasures().map { it.typeName }

fun TypeDescription.qualifiedNameOrNull(): String? =
    typeName.takeIf { it.contains(".") }

fun Generic.toPTypeEntry(): PTypeEntry = when (sort) {
    TypeDefinition.Sort.GENERIC_ARRAY -> {
        val component = componentType
            ?: throw IllegalStateException("Generic array type without component: $this")
        PGenericArrayTypeEntry(component.toPTypeEntry())
    }
    TypeDefinition.Sort.PARAMETERIZED -> PParameterizedTypeEntry(
        rawTypeName = asErasure().typeName,
        ownerType = ownerType?.toPTypeEntry(),
        arguments = typeArguments.map { it.toPTypeEntry() }
    )
    TypeDefinition.Sort.WILDCARD -> PWildcardTypeEntry(
        upperBounds = upperBounds.map { it.toPTypeEntry() },
        lowerBounds = lowerBounds.map { it.toPTypeEntry() }
    )
    TypeDefinition.Sort.VARIABLE -> PTypeVariableEntry(
        name = symbol,
        bounds = upperBounds.map { it.toPTypeEntry() }
    )
    else -> PClassTypeEntry(asErasure().typeName)
}
