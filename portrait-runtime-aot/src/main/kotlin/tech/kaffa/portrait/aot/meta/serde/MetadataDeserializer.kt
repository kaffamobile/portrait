package tech.kaffa.portrait.aot.meta.serde

import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.PConstructorEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.*

class MetadataDeserializer {

    fun deserialize(data: String): PClassEntry {
        val bytes = Base64.getDecoder().decode(data)
        val input = ByteArrayInputStream(bytes)
        val stream = DataInputStream(input)

        // Read and validate header
        val magic = stream.readInt()
        if (magic != MetadataSerializer.MAGIC_NUMBER) {
            throw IllegalArgumentException("Invalid magic number: 0x${magic.toString(16)}")
        }

        val version = stream.readInt()
        if (version != MetadataSerializer.VERSION) {
            throw IllegalArgumentException("Unsupported version: $version")
        }

        // Read string pool
        val stringPool = readStringPool(stream)

        // Read single class
        return readClass(stream, stringPool)
    }

    private fun readStringPool(stream: DataInputStream): ReadOnlyStringPool {
        val stringCount = stream.readInt()
        val strings = (0 until stringCount).map { stream.readUTF() }
        return ReadOnlyStringPool(strings)
    }

    private fun readClass(stream: DataInputStream, stringPool: ReadOnlyStringPool): PClassEntry {
        val simpleName = stringPool.getString(stream.readInt())
        val qualifiedNameIndex = stream.readInt()
        val qualifiedName = if (qualifiedNameIndex == -1) null else stringPool.getString(qualifiedNameIndex)
        val flags = stream.readInt()
        val javaClassName = stringPool.getString(stream.readInt())
        val superclassNameIndex = stream.readInt()
        val superclassName = if (superclassNameIndex == -1) null else stringPool.getString(superclassNameIndex)

        // Interface names
        val interfaceCount = stream.readInt()
        val interfaceNames = (0 until interfaceCount).map { stringPool.getString(stream.readInt()) }

        // Annotations
        val annotations = readAnnotations(stream, stringPool)

        // Constructors
        val constructorCount = stream.readInt()
        val constructors = (0 until constructorCount).map { readConstructor(stream, stringPool) }

        // Methods
        val methodCount = stream.readInt()
        val methods = (0 until methodCount).map { readMethod(stream, stringPool) }

        // Proxy methods
        val proxyMethodCount = stream.readInt()
        val proxyMethods = (0 until proxyMethodCount).map { readMethod(stream, stringPool) }

        // Fields
        val fieldCount = stream.readInt()
        val fields = (0 until fieldCount).map { readField(stream, stringPool) }

        return PClassEntry(
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            isAbstract = (flags and ClassFlags.IS_ABSTRACT) != 0,
            isSealed = (flags and ClassFlags.IS_SEALED) != 0,
            isData = (flags and ClassFlags.IS_DATA) != 0,
            isCompanion = (flags and ClassFlags.IS_COMPANION) != 0,
            isObject = (flags and ClassFlags.IS_OBJECT) != 0,
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

    private fun readConstructor(stream: DataInputStream, stringPool: ReadOnlyStringPool): PConstructorEntry {
        val declaringClassName = stringPool.getString(stream.readInt())
        val flags = stream.readInt()

        // Parameter types
        val parameterCount = stream.readInt()
        val parameterTypeNames = (0 until parameterCount).map { stringPool.getString(stream.readInt()) }

        val annotations = readAnnotations(stream, stringPool)

        return PConstructorEntry(
            declaringClassName = declaringClassName,
            parameterTypeNames = parameterTypeNames,
            annotations = annotations,
            isPublic = (flags and ConstructorFlags.IS_PUBLIC) != 0,
            isPrivate = (flags and ConstructorFlags.IS_PRIVATE) != 0,
            isProtected = (flags and ConstructorFlags.IS_PROTECTED) != 0
        )
    }

    private fun readField(stream: DataInputStream, stringPool: ReadOnlyStringPool): PFieldEntry {
        val name = stringPool.getString(stream.readInt())
        val typeName = stringPool.getString(stream.readInt())
        val declaringClassName = stringPool.getString(stream.readInt())
        val flags = stream.readInt()

        val annotations = readAnnotations(stream, stringPool)

        return PFieldEntry(
            name = name,
            typeName = typeName,
            declaringClassName = declaringClassName,
            isPublic = (flags and FieldFlags.IS_PUBLIC) != 0,
            isPrivate = (flags and FieldFlags.IS_PRIVATE) != 0,
            isProtected = (flags and FieldFlags.IS_PROTECTED) != 0,
            isStatic = (flags and FieldFlags.IS_STATIC) != 0,
            isFinal = (flags and FieldFlags.IS_FINAL) != 0,
            annotations = annotations
        )
    }

    private fun readMethod(stream: DataInputStream, stringPool: ReadOnlyStringPool): PMethodEntry {
        val name = stringPool.getString(stream.readInt())
        val returnTypeName = stringPool.getString(stream.readInt())
        val declaringClassName = stringPool.getString(stream.readInt())
        val flags = stream.readInt()

        // Parameter types
        val parameterCount = stream.readInt()
        val parameterTypeNames = (0 until parameterCount).map { stringPool.getString(stream.readInt()) }

        val annotations = readAnnotations(stream, stringPool)

        // Parameter annotations
        val parameterAnnotationCount = stream.readInt()
        val parameterAnnotations = (0 until parameterAnnotationCount).map {
            readAnnotations(stream, stringPool)
        }

        return PMethodEntry(
            name = name,
            parameterTypeNames = parameterTypeNames,
            returnTypeName = returnTypeName,
            declaringClassName = declaringClassName,
            isPublic = (flags and MethodFlags.IS_PUBLIC) != 0,
            isPrivate = (flags and MethodFlags.IS_PRIVATE) != 0,
            isProtected = (flags and MethodFlags.IS_PROTECTED) != 0,
            isStatic = (flags and MethodFlags.IS_STATIC) != 0,
            isFinal = (flags and MethodFlags.IS_FINAL) != 0,
            isAbstract = (flags and MethodFlags.IS_ABSTRACT) != 0,
            annotations = annotations,
            parameterAnnotations = parameterAnnotations
        )
    }

    private fun readAnnotations(stream: DataInputStream, stringPool: ReadOnlyStringPool): List<PAnnotationEntry> {
        val annotationCount = stream.readInt()
        return (0 until annotationCount).map { readAnnotation(stream, stringPool) }
    }

    private fun readAnnotation(stream: DataInputStream, stringPool: ReadOnlyStringPool): PAnnotationEntry {
        val annotationClassName = stringPool.getString(stream.readInt())
        val simpleName = stringPool.getString(stream.readInt())
        val qualifiedNameIndex = stream.readInt()
        val qualifiedName = if (qualifiedNameIndex == -1) null else stringPool.getString(qualifiedNameIndex)

        // Properties
        val propertyCount = stream.readInt()
        val properties = mutableMapOf<String, Any?>()
        repeat(propertyCount) {
            val key = stringPool.getString(stream.readInt())
            val value = readAnnotationValue(stream, stringPool)
            properties[key] = value
        }

        return PAnnotationEntry(
            annotationClassName = annotationClassName,
            simpleName = simpleName,
            qualifiedName = qualifiedName,
            properties = properties
        )
    }

    private fun readAnnotationValue(stream: DataInputStream, stringPool: ReadOnlyStringPool): Any? {
        return when (val type = stream.readByte().toInt()) {
            MetadataSerializer.TYPE_NULL -> null
            MetadataSerializer.TYPE_STRING -> stringPool.getString(stream.readInt())
            MetadataSerializer.TYPE_BOOLEAN -> stream.readBoolean()
            MetadataSerializer.TYPE_INT -> stream.readInt()
            MetadataSerializer.TYPE_LONG -> stream.readLong()
            MetadataSerializer.TYPE_FLOAT -> stream.readFloat()
            MetadataSerializer.TYPE_DOUBLE -> stream.readDouble()
            MetadataSerializer.TYPE_LIST -> {
                val size = stream.readInt()
                (0 until size).map { readAnnotationValue(stream, stringPool) }
            }

            MetadataSerializer.TYPE_OTHER -> stream.readUTF()
            else -> throw IllegalArgumentException("Unknown annotation value type: $type")
        }
    }
}
