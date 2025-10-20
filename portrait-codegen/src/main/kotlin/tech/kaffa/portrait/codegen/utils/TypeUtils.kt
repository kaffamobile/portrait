package tech.kaffa.portrait.codegen.utils

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription

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
