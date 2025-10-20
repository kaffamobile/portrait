package tech.kaffa.portrait.aot.meta.serde

import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.PConstructorEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry

object ClassFlags {
    const val IS_ABSTRACT = 1 shl 0
    const val IS_SEALED = 1 shl 1
    const val IS_DATA = 1 shl 2
    const val IS_COMPANION = 1 shl 3
    const val IS_OBJECT = 1 shl 4
    const val IS_ENUM = 1 shl 5
    const val HAS_SUPERCLASS = 1 shl 6
}

object FieldFlags {
    const val IS_STATIC = 1 shl 0
    const val IS_FINAL = 1 shl 1
}

object MethodFlags {
    const val IS_STATIC = 1 shl 0
    const val IS_FINAL = 1 shl 1
    const val IS_ABSTRACT = 1 shl 2
}

fun buildClassFlags(entry: PClassEntry): Int {
    var flags = 0
    if (entry.isAbstract) flags = flags or ClassFlags.IS_ABSTRACT
    if (entry.isSealed) flags = flags or ClassFlags.IS_SEALED
    if (entry.isData) flags = flags or ClassFlags.IS_DATA
    if (entry.isCompanion) flags = flags or ClassFlags.IS_COMPANION
    if (entry.isObject) flags = flags or ClassFlags.IS_OBJECT
    if (entry.isEnum) flags = flags or ClassFlags.IS_ENUM
    if (entry.superclassName != null) flags = flags or ClassFlags.HAS_SUPERCLASS
    return flags
}

fun buildFieldFlags(entry: PFieldEntry): Int {
    var flags = 0
    if (entry.isStatic) flags = flags or FieldFlags.IS_STATIC
    if (entry.isFinal) flags = flags or FieldFlags.IS_FINAL
    return flags
}

fun buildMethodFlags(entry: PMethodEntry): Int {
    var flags = 0
    if (entry.isStatic) flags = flags or MethodFlags.IS_STATIC
    if (entry.isFinal) flags = flags or MethodFlags.IS_FINAL
    if (entry.isAbstract) flags = flags or MethodFlags.IS_ABSTRACT
    return flags
}
