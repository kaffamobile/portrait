package tech.kaffa.portrait.aot.meta.serde

import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.PConstructorEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry
import tech.kaffa.portrait.aot.meta.PTypeEntry
import tech.kaffa.portrait.aot.meta.PClassTypeEntry
import tech.kaffa.portrait.aot.meta.PParameterizedTypeEntry
import tech.kaffa.portrait.aot.meta.PTypeVariableEntry
import tech.kaffa.portrait.aot.meta.PWildcardTypeEntry
import tech.kaffa.portrait.aot.meta.PGenericArrayTypeEntry
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.Base64

class MetadataDeserializer {

    fun deserialize(data: String): PClassEntry {
        val bytes = Base64.getDecoder().decode(data)
        val input = ByteArrayInputStream(bytes)
        val stream = DataInputStream(input)

        val version = stream.readUnsignedShort()
        if (version != MetadataSerializer.VERSION) {
            throw IllegalArgumentException("Unsupported version: $version")
        }

        val stringWidth = IntWidth.fromId(stream.readUnsignedByte())
        val stringCount = stringWidth.read(stream)
        val strings = (0 until stringCount).map { stream.readUTF() }
        val stringPool = ReadOnlyStringPool(strings)
        val context = DecodingContext(stringPool, stringWidth)

        return readClass(stream, context)
    }

    private fun readClass(stream: DataInputStream, context: DecodingContext): PClassEntry {
        val simpleName = context.readString(stream)
        val qualifiedName = context.readString(stream)
        val flags = stream.readUnsignedByte()
        val typeId = flags and ClassFlags.TYPE_MASK
        val modifierId = (flags shr ClassFlags.MODIFIER_SHIFT) and ClassFlags.MODIFIER_MASK
        val collectionWidthId = (flags shr ClassFlags.COLLECTION_WIDTH_SHIFT) and ClassFlags.COLLECTION_WIDTH_VALUE_MASK
        val hasProxyMethods = (flags and ClassFlags.HAS_PROXY_METHODS) != 0
        val collectionWidth = IntWidth.fromId(collectionWidthId)
        val javaClassName = context.readString(stream)
        val superclassName = if ((flags and ClassFlags.HAS_SUPERCLASS) != 0) {
            context.readString(stream)
        } else {
            null
        }

        val decoded = decodeClassType(typeId, modifierId)

        // Interface names
        val interfaceCount = collectionWidth.read(stream)
        val interfaceNames = (0 until interfaceCount).map { context.readString(stream) }

        // Annotations
        val annotations = readAnnotations(stream, context)

        // Constructors
        val constructorCount = collectionWidth.read(stream)
        val constructors = (0 until constructorCount).map { readConstructor(stream, context) }

        // Methods
        val methodCount = collectionWidth.read(stream)
        val methods = (0 until methodCount).map { readMethod(stream, context) }

        // Fields
        val fieldCount = collectionWidth.read(stream)
        val fields = (0 until fieldCount).map { readField(stream, context) }

        // Proxy methods
        val proxyMethods = if (hasProxyMethods) {
            val proxyMethodCount = stream.readInt()
            (0 until proxyMethodCount).map { readMethod(stream, context) }
        } else {
            emptyList()
        }

        return PClassEntry(
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            isAbstract = decoded.isAbstract,
            isSealed = decoded.isSealed,
            isData = decoded.isData,
            isCompanion = decoded.isCompanion,
            isObject = decoded.isObject,
            isEnum = decoded.isEnum,
            isInterface = decoded.isInterface,
            javaClassName = javaClassName,
            superclassName = superclassName,
            interfaceNames = interfaceNames,
            annotations = annotations,
            constructors = constructors,
            declaredMethods = methods,
            declaredFields = fields,
            proxyMethods = proxyMethods
        )
    }

    private fun readConstructor(stream: DataInputStream, context: DecodingContext): PConstructorEntry {
        val declaringClassName = context.readString(stream)

        val parameterWidth = IntWidth.fromId(stream.readUnsignedByte())
        val parameterCount = parameterWidth.read(stream)
        val parameterTypeNames = (0 until parameterCount).map { context.readString(stream) }

        val annotations = readAnnotations(stream, context)

        return PConstructorEntry(
            declaringClassName = declaringClassName,
            parameterTypeNames = parameterTypeNames,
            annotations = annotations
        )
    }

    private fun readField(stream: DataInputStream, context: DecodingContext): PFieldEntry {
        val name = context.readString(stream)
        val typeName = context.readString(stream)
        val declaringClassName = context.readString(stream)
        val flags = stream.readUnsignedByte()

        val annotations = readAnnotations(stream, context)

        return PFieldEntry(
            name = name,
            typeName = typeName,
            declaringClassName = declaringClassName,
            isStatic = (flags and FieldFlags.IS_STATIC) != 0,
            isFinal = (flags and FieldFlags.IS_FINAL) != 0,
            annotations = annotations
        )
    }

    private fun readMethod(stream: DataInputStream, context: DecodingContext): PMethodEntry {
        val name = context.readString(stream)
        val returnTypeName = context.readString(stream)
        val genericReturnType = readType(stream, context)
        val declaringClassName = context.readString(stream)
        val flags = stream.readUnsignedByte()
        val parameterWidthId = (flags shr MethodFlags.PARAM_WIDTH_SHIFT) and MethodFlags.PARAM_WIDTH_VALUE_MASK
        val parameterWidth = IntWidth.fromId(parameterWidthId)

        val parameterCount = parameterWidth.read(stream)
        val parameterTypeNames = (0 until parameterCount).map { context.readString(stream) }

        val annotations = readAnnotations(stream, context)

        val parameterAnnotationCount = parameterWidth.read(stream)
        val parameterAnnotations = (0 until parameterAnnotationCount).map {
            readAnnotations(stream, context)
        }

        return PMethodEntry(
            name = name,
            parameterTypeNames = parameterTypeNames,
            returnTypeName = returnTypeName,
            genericReturnType = genericReturnType,
            declaringClassName = declaringClassName,
            isStatic = (flags and MethodFlags.IS_STATIC) != 0,
            isFinal = (flags and MethodFlags.IS_FINAL) != 0,
            isAbstract = (flags and MethodFlags.IS_ABSTRACT) != 0,
            annotations = annotations,
            parameterAnnotations = parameterAnnotations
        )
    }

    private fun readType(stream: DataInputStream, context: DecodingContext): PTypeEntry {
        return when (val kind = stream.readUnsignedByte()) {
            MetadataSerializer.GENERIC_TYPE_CLASS -> {
                val className = context.readString(stream)
                PClassTypeEntry(className)
            }
            MetadataSerializer.GENERIC_TYPE_PARAMETERIZED -> {
                val rawTypeName = context.readString(stream)
                val ownerType = if (stream.readBoolean()) {
                    readType(stream, context)
                } else {
                    null
                }
                val argWidth = IntWidth.fromId(stream.readUnsignedByte())
                val argCount = argWidth.read(stream)
                val arguments = (0 until argCount).map { readType(stream, context) }
                PParameterizedTypeEntry(
                    rawTypeName = rawTypeName,
                    ownerType = ownerType,
                    arguments = arguments
                )
            }
            MetadataSerializer.GENERIC_TYPE_VARIABLE -> {
                val name = context.readString(stream)
                val boundsWidth = IntWidth.fromId(stream.readUnsignedByte())
                val boundsCount = boundsWidth.read(stream)
                val bounds = (0 until boundsCount).map { readType(stream, context) }
                PTypeVariableEntry(
                    name = name,
                    bounds = bounds
                )
            }
            MetadataSerializer.GENERIC_TYPE_WILDCARD -> {
                val upperWidth = IntWidth.fromId(stream.readUnsignedByte())
                val upperCount = upperWidth.read(stream)
                val upperBounds = (0 until upperCount).map { readType(stream, context) }

                val lowerWidth = IntWidth.fromId(stream.readUnsignedByte())
                val lowerCount = lowerWidth.read(stream)
                val lowerBounds = (0 until lowerCount).map { readType(stream, context) }

                PWildcardTypeEntry(
                    upperBounds = upperBounds,
                    lowerBounds = lowerBounds
                )
            }
            MetadataSerializer.GENERIC_TYPE_GENERIC_ARRAY -> {
                val componentType = readType(stream, context)
                PGenericArrayTypeEntry(componentType)
            }
            else -> throw IllegalArgumentException("Unknown generic type marker: $kind")
        }
    }

    private fun readAnnotations(stream: DataInputStream, context: DecodingContext): List<PAnnotationEntry> {
        val annotationCount = stream.readInt()
        return (0 until annotationCount).map { readAnnotation(stream, context) }
    }

    private fun readAnnotation(stream: DataInputStream, context: DecodingContext): PAnnotationEntry {
        val annotationClassName = context.readString(stream)
        val simpleName = context.readString(stream)
        val qualifiedName = if (stream.readBoolean()) {
            context.readString(stream)
        } else {
            null
        }

        val propertyCount = stream.readInt()
        val properties = mutableMapOf<String, Any?>()
        repeat(propertyCount) {
            val key = context.readString(stream)
            val value = readAnnotationValue(stream, context)
            properties[key] = value
        }

        return PAnnotationEntry(
            annotationClassName = annotationClassName,
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            properties = properties
        )
    }

    private fun readAnnotationValue(stream: DataInputStream, context: DecodingContext): Any? {
        return when (val type = stream.readUnsignedByte()) {
            MetadataSerializer.TYPE_NULL -> null
            MetadataSerializer.TYPE_STRING -> context.readString(stream)
            MetadataSerializer.TYPE_BOOLEAN -> stream.readBoolean()
            MetadataSerializer.TYPE_INT -> stream.readInt()
            MetadataSerializer.TYPE_LONG -> stream.readLong()
            MetadataSerializer.TYPE_FLOAT -> stream.readFloat()
            MetadataSerializer.TYPE_DOUBLE -> stream.readDouble()
            MetadataSerializer.TYPE_LIST_U8 -> readAnnotationList(stream, context, IntWidth.U8)
            MetadataSerializer.TYPE_LIST_U16 -> readAnnotationList(stream, context, IntWidth.U16)
            MetadataSerializer.TYPE_LIST_U24 -> readAnnotationList(stream, context, IntWidth.U24)
            MetadataSerializer.TYPE_LIST_INT -> readAnnotationList(stream, context, IntWidth.U32)
            MetadataSerializer.TYPE_OTHER -> stream.readUTF()
            else -> throw IllegalArgumentException("Unknown annotation value type: $type")
        }
    }

    private fun readAnnotationList(
        stream: DataInputStream,
        context: DecodingContext,
        width: IntWidth
    ): List<Any?> {
        val size = width.read(stream)
        return (0 until size).map {
            readAnnotationValue(stream, context)
        }
    }

    private fun decodeClassType(typeId: Int, modifierId: Int): DecodedClassFlags {
        var isAbstract = false
        var isSealed = false
        var isData = false
        var isCompanion = false
        var isObject = false
        var isEnum = false
        var isInterface = false

        when (typeId) {
            ClassFlags.TYPE_CLASS -> {
                when (modifierId) {
                    ClassFlags.CLASS_MOD_NONE -> Unit
                    ClassFlags.CLASS_MOD_ABSTRACT -> isAbstract = true
                    ClassFlags.CLASS_MOD_SEALED -> {
                        isSealed = true
                        isAbstract = true
                    }
                    ClassFlags.CLASS_MOD_DATA -> isData = true
                    else -> error("Unsupported class modifier id: $modifierId")
                }
            }
            ClassFlags.TYPE_INTERFACE -> {
                isInterface = true
                isAbstract = true
                if (modifierId == ClassFlags.INTERFACE_MOD_SEALED) {
                    isSealed = true
                } else if (modifierId != ClassFlags.INTERFACE_MOD_NONE) {
                    error("Unsupported interface modifier id: $modifierId")
                }
            }
            ClassFlags.TYPE_ENUM -> {
                isEnum = true
                if (modifierId != ClassFlags.CLASS_MOD_NONE) {
                    error("Unsupported enum modifier id: $modifierId")
                }
            }
            ClassFlags.TYPE_OBJECT -> {
                isObject = true
                when (modifierId) {
                    ClassFlags.OBJECT_MOD_NONE -> Unit
                    ClassFlags.OBJECT_MOD_COMPANION -> isCompanion = true
                    ClassFlags.OBJECT_MOD_DATA -> isData = true
                    else -> error("Unsupported object modifier id: $modifierId")
                }
            }
            else -> error("Unsupported class type id: $typeId")
        }

        return DecodedClassFlags(
            isAbstract = isAbstract,
            isSealed = isSealed,
            isData = isData,
            isCompanion = isCompanion,
            isObject = isObject,
            isEnum = isEnum,
            isInterface = isInterface
        )
    }

    private data class DecodedClassFlags(
        val isAbstract: Boolean,
        val isSealed: Boolean,
        val isData: Boolean,
        val isCompanion: Boolean,
        val isObject: Boolean,
        val isEnum: Boolean,
        val isInterface: Boolean
    )

    private data class DecodingContext(
        val stringPool: ReadOnlyStringPool,
        val stringWidth: IntWidth
    ) {
        fun readIndex(stream: DataInputStream): Int = stringWidth.read(stream)

        fun readString(stream: DataInputStream): String =
            stringPool.getString(readIndex(stream))
    }

}
