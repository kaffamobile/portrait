package kaffa.portrait.aot.meta.serde

import kaffa.portrait.aot.meta.PClassEntry
import kaffa.portrait.aot.meta.PConstructorEntry
import kaffa.portrait.aot.meta.PFieldEntry
import kaffa.portrait.aot.meta.PMethodEntry

object ClassFlags {
    const val IS_ABSTRACT = 1 shl 0
    const val IS_SEALED = 1 shl 1
    const val IS_DATA = 1 shl 2
    const val IS_COMPANION = 1 shl 3
    const val IS_OBJECT = 1 shl 4
    const val HAS_SUPERCLASS = 1 shl 5
}

object ConstructorFlags {
    const val IS_ACCESSIBLE = 1 shl 0
    const val IS_PUBLIC = 1 shl 1
    const val IS_PRIVATE = 1 shl 2
    const val IS_PROTECTED = 1 shl 3
    const val IS_INTERNAL = 1 shl 4
}

object FieldFlags {
    const val IS_PUBLIC = 1 shl 0
    const val IS_PRIVATE = 1 shl 1
    const val IS_PROTECTED = 1 shl 2
    const val IS_STATIC = 1 shl 3
    const val IS_FINAL = 1 shl 4
    const val IS_TRANSIENT = 1 shl 5
    const val IS_VOLATILE = 1 shl 6
}

object MethodFlags {
    const val IS_PUBLIC = 1 shl 0
    const val IS_PRIVATE = 1 shl 1
    const val IS_PROTECTED = 1 shl 2
    const val IS_STATIC = 1 shl 3
    const val IS_FINAL = 1 shl 4
    const val IS_ABSTRACT = 1 shl 5
}

fun buildClassFlags(entry: PClassEntry): Int {
    var flags = 0
    if (entry.isAbstract) flags = flags or ClassFlags.IS_ABSTRACT
    if (entry.isSealed) flags = flags or ClassFlags.IS_SEALED
    if (entry.isData) flags = flags or ClassFlags.IS_DATA
    if (entry.isCompanion) flags = flags or ClassFlags.IS_COMPANION
    if (entry.isObject) flags = flags or ClassFlags.IS_OBJECT
    if (entry.superclassName != null) flags = flags or ClassFlags.HAS_SUPERCLASS
    return flags
}

fun buildConstructorFlags(entry: PConstructorEntry): Int {
    var flags = 0
    if (entry.isPublic) flags = flags or ConstructorFlags.IS_PUBLIC
    if (entry.isPrivate) flags = flags or ConstructorFlags.IS_PRIVATE
    if (entry.isProtected) flags = flags or ConstructorFlags.IS_PROTECTED
    return flags
}

fun buildFieldFlags(entry: PFieldEntry): Int {
    var flags = 0
    if (entry.isPublic) flags = flags or FieldFlags.IS_PUBLIC
    if (entry.isPrivate) flags = flags or FieldFlags.IS_PRIVATE
    if (entry.isProtected) flags = flags or FieldFlags.IS_PROTECTED
    if (entry.isStatic) flags = flags or FieldFlags.IS_STATIC
    if (entry.isFinal) flags = flags or FieldFlags.IS_FINAL
    return flags
}

fun buildMethodFlags(entry: PMethodEntry): Int {
    var flags = 0
    if (entry.isPublic) flags = flags or MethodFlags.IS_PUBLIC
    if (entry.isPrivate) flags = flags or MethodFlags.IS_PRIVATE
    if (entry.isProtected) flags = flags or MethodFlags.IS_PROTECTED
    if (entry.isStatic) flags = flags or MethodFlags.IS_STATIC
    if (entry.isFinal) flags = flags or MethodFlags.IS_FINAL
    if (entry.isAbstract) flags = flags or MethodFlags.IS_ABSTRACT
    return flags
}