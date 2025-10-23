package tech.kaffa.portrait.aot.meta.serde

import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry

object ClassFlags {
    const val TYPE_MASK = 0x3
    const val TYPE_CLASS = 0
    const val TYPE_INTERFACE = 1
    const val TYPE_ENUM = 2
    const val TYPE_OBJECT = 3

    const val MODIFIER_SHIFT = 2
    const val MODIFIER_MASK = 0x3

    const val COLLECTION_WIDTH_SHIFT = 4
    const val COLLECTION_WIDTH_VALUE_MASK = 0x3

    const val HAS_PROXY_METHODS = 1 shl 6
    const val HAS_SUPERCLASS = 1 shl 7

    const val CLASS_MOD_NONE = 0
    const val CLASS_MOD_ABSTRACT = 1
    const val CLASS_MOD_SEALED = 2
    const val CLASS_MOD_DATA = 3

    const val INTERFACE_MOD_NONE = 0
    const val INTERFACE_MOD_SEALED = 1

    const val OBJECT_MOD_NONE = 0
    const val OBJECT_MOD_COMPANION = 1
    const val OBJECT_MOD_DATA = 2
}

object FieldFlags {
    const val IS_STATIC = 1 shl 0
    const val IS_FINAL = 1 shl 1
}

object MethodFlags {
    const val IS_STATIC = 1 shl 0
    const val IS_FINAL = 1 shl 1
    const val IS_ABSTRACT = 1 shl 2
    const val PARAM_WIDTH_SHIFT = 3
    const val PARAM_WIDTH_VALUE_MASK = 0x3
}

fun buildClassFlags(entry: PClassEntry, collectionWidth: IntWidth): Int {
    val typeBits = when {
        entry.isInterface -> ClassFlags.TYPE_INTERFACE
        entry.isEnum -> ClassFlags.TYPE_ENUM
        entry.isObject -> ClassFlags.TYPE_OBJECT
        else -> ClassFlags.TYPE_CLASS
    }

    val modifierBits = when (typeBits) {
        ClassFlags.TYPE_CLASS -> when {
            entry.isSealed -> ClassFlags.CLASS_MOD_SEALED
            entry.isData -> ClassFlags.CLASS_MOD_DATA
            entry.isAbstract -> ClassFlags.CLASS_MOD_ABSTRACT
            else -> ClassFlags.CLASS_MOD_NONE
        }
        ClassFlags.TYPE_INTERFACE -> if (entry.isSealed) {
            ClassFlags.INTERFACE_MOD_SEALED
        } else {
            ClassFlags.INTERFACE_MOD_NONE
        }
        ClassFlags.TYPE_OBJECT -> when {
            entry.isCompanion -> ClassFlags.OBJECT_MOD_COMPANION
            entry.isData -> ClassFlags.OBJECT_MOD_DATA
            else -> ClassFlags.OBJECT_MOD_NONE
        }
        ClassFlags.TYPE_ENUM -> ClassFlags.CLASS_MOD_NONE
        else -> error("Unsupported class type bits: $typeBits")
    }

    var flags = typeBits
    flags = flags or (modifierBits shl ClassFlags.MODIFIER_SHIFT)
    flags = flags or (collectionWidth.id shl ClassFlags.COLLECTION_WIDTH_SHIFT)

    if (entry.superclassName != null) {
        flags = flags or ClassFlags.HAS_SUPERCLASS
    }
    if (entry.proxyMethods.isNotEmpty()) {
        flags = flags or ClassFlags.HAS_PROXY_METHODS
    }
    return flags
}

fun buildFieldFlags(entry: PFieldEntry): Int {
    var flags = 0
    if (entry.isStatic) flags = flags or FieldFlags.IS_STATIC
    if (entry.isFinal) flags = flags or FieldFlags.IS_FINAL
    return flags
}

fun buildMethodFlags(
    entry: PMethodEntry,
    parameterWidth: IntWidth
): Int {
    var flags = 0
    if (entry.isStatic) flags = flags or MethodFlags.IS_STATIC
    if (entry.isFinal) flags = flags or MethodFlags.IS_FINAL
    if (entry.isAbstract) flags = flags or MethodFlags.IS_ABSTRACT
    flags = flags or (parameterWidth.id shl MethodFlags.PARAM_WIDTH_SHIFT)
    return flags
}
