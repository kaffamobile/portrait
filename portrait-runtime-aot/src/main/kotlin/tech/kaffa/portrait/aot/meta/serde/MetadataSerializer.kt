package tech.kaffa.portrait.aot.meta.serde

import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.PConstructorEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.*

class MetadataSerializer {
    private val stringPool = StringPool()

    fun serialize(clazz: PClassEntry): String {
        val output = ByteArrayOutputStream()
        val data = DataOutputStream(output)

        // First pass: collect all strings
        collectClassStrings(clazz)

        // Write header
        data.writeInt(MAGIC_NUMBER)
        data.writeInt(VERSION)

        // Write string pool
        writeStringPool(data)

        // Write single class
        writeClass(data, clazz)

        // Convert to Base64 string
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }


    private fun collectClassStrings(clazz: PClassEntry) {
        stringPool.intern(clazz.simpleName)
        stringPool.intern(clazz.qualifiedName)
        stringPool.intern(clazz.javaClassName)
        clazz.superclassName?.let { stringPool.intern(it) }
        clazz.interfaceNames.forEach { stringPool.intern(it) }

        clazz.annotations.forEach { collectAnnotationStrings(it) }
        clazz.constructors.forEach { collectConstructorStrings(it) }
        clazz.declaredMethods.forEach { collectMethodStrings(it) }
        clazz.declaredFields.forEach { collectFieldStrings(it) }
        clazz.proxyMethods.forEach { collectMethodStrings(it) }
    }

    private fun collectConstructorStrings(constructor: PConstructorEntry) {
        stringPool.intern(constructor.declaringClassName)
        constructor.parameterTypeNames.forEach { stringPool.intern(it) }
        constructor.annotations.forEach { collectAnnotationStrings(it) }
    }

    private fun collectFieldStrings(field: PFieldEntry) {
        stringPool.intern(field.name)
        stringPool.intern(field.typeName)
        stringPool.intern(field.declaringClassName)
        field.annotations.forEach { collectAnnotationStrings(it) }
    }

    private fun collectMethodStrings(method: PMethodEntry) {
        stringPool.intern(method.name)
        method.parameterTypeNames.forEach { stringPool.intern(it) }
        stringPool.intern(method.returnTypeName)
        stringPool.intern(method.declaringClassName)
        method.annotations.forEach { collectAnnotationStrings(it) }
        method.parameterAnnotations.forEach { annotations ->
            annotations.forEach { collectAnnotationStrings(it) }
        }
    }

    private fun collectAnnotationStrings(annotation: PAnnotationEntry) {
        stringPool.intern(annotation.annotationClassName)
        stringPool.intern(annotation.simpleName)
        annotation.qualifiedName?.let { stringPool.intern(it) }
        annotation.properties.keys.forEach { stringPool.intern(it) }
        annotation.properties.values.forEach { value ->
            when (value) {
                is String -> stringPool.intern(value)
                is List<*> -> value.forEach { item ->
                    if (item is String) stringPool.intern(item)
                }
            }
        }
    }

    private fun writeStringPool(data: DataOutputStream) {
        val strings = stringPool.getStrings()
        data.writeInt(strings.size)
        strings.forEach { data.writeUTF(it) }
    }

    private fun writeClass(data: DataOutputStream, clazz: PClassEntry) {
        data.writeInt(stringPool.intern(clazz.simpleName))
        data.writeInt(stringPool.intern(clazz.qualifiedName))
        data.writeInt(buildClassFlags(clazz))
        data.writeInt(stringPool.intern(clazz.javaClassName))
        data.writeInt(clazz.superclassName?.let { stringPool.intern(it) } ?: -1)

        // Interface names
        data.writeInt(clazz.interfaceNames.size)
        clazz.interfaceNames.forEach { data.writeInt(stringPool.intern(it)) }

        // Annotations
        writeAnnotations(data, clazz.annotations)

        // Constructors
        data.writeInt(clazz.constructors.size)
        clazz.constructors.forEach { writeConstructor(data, it) }

        // Methods
        data.writeInt(clazz.declaredMethods.size)
        clazz.declaredMethods.forEach { writeMethod(data, it) }

        // Proxy methods
        data.writeInt(clazz.proxyMethods.size)
        clazz.proxyMethods.forEach { writeMethod(data, it) }

        // Fields
        data.writeInt(clazz.declaredFields.size)
        clazz.declaredFields.forEach { writeField(data, it) }
    }

    private fun writeConstructor(data: DataOutputStream, constructor: PConstructorEntry) {
        data.writeInt(stringPool.intern(constructor.declaringClassName))
        data.writeInt(buildConstructorFlags(constructor))

        // Parameter types
        data.writeInt(constructor.parameterTypeNames.size)
        constructor.parameterTypeNames.forEach { data.writeInt(stringPool.intern(it)) }

        writeAnnotations(data, constructor.annotations)
    }

    private fun writeField(data: DataOutputStream, field: PFieldEntry) {
        data.writeInt(stringPool.intern(field.name))
        data.writeInt(stringPool.intern(field.typeName))
        data.writeInt(stringPool.intern(field.declaringClassName))
        data.writeInt(buildFieldFlags(field))

        writeAnnotations(data, field.annotations)
    }

    private fun writeMethod(data: DataOutputStream, method: PMethodEntry) {
        data.writeInt(stringPool.intern(method.name))
        data.writeInt(stringPool.intern(method.returnTypeName))
        data.writeInt(stringPool.intern(method.declaringClassName))
        data.writeInt(buildMethodFlags(method))

        // Parameter types
        data.writeInt(method.parameterTypeNames.size)
        method.parameterTypeNames.forEach { data.writeInt(stringPool.intern(it)) }

        writeAnnotations(data, method.annotations)

        // Parameter annotations
        data.writeInt(method.parameterAnnotations.size)
        method.parameterAnnotations.forEach { annotations ->
            writeAnnotations(data, annotations)
        }
    }

    private fun writeAnnotations(data: DataOutputStream, annotations: List<PAnnotationEntry>) {
        data.writeInt(annotations.size)
        annotations.forEach { writeAnnotation(data, it) }
    }

    private fun writeAnnotation(data: DataOutputStream, annotation: PAnnotationEntry) {
        data.writeInt(stringPool.intern(annotation.annotationClassName))
        data.writeInt(stringPool.intern(annotation.simpleName))
        data.writeInt(annotation.qualifiedName?.let { stringPool.intern(it) } ?: -1)

        // Properties
        data.writeInt(annotation.properties.size)
        annotation.properties.forEach { (key, value) ->
            data.writeInt(stringPool.intern(key))
            writeAnnotationValue(data, value)
        }
    }

    private fun writeAnnotationValue(data: DataOutputStream, value: Any?) {
        when (value) {
            null -> {
                data.writeByte(TYPE_NULL)
            }

            is String -> {
                data.writeByte(TYPE_STRING)
                data.writeInt(stringPool.intern(value))
            }

            is Boolean -> {
                data.writeByte(TYPE_BOOLEAN)
                data.writeBoolean(value)
            }

            is Int -> {
                data.writeByte(TYPE_INT)
                data.writeInt(value)
            }

            is Long -> {
                data.writeByte(TYPE_LONG)
                data.writeLong(value)
            }

            is Float -> {
                data.writeByte(TYPE_FLOAT)
                data.writeFloat(value)
            }

            is Double -> {
                data.writeByte(TYPE_DOUBLE)
                data.writeDouble(value)
            }

            is List<*> -> {
                data.writeByte(TYPE_LIST)
                data.writeInt(value.size)
                value.forEach { writeAnnotationValue(data, it) }
            }

            else -> {
                data.writeByte(TYPE_OTHER)
                data.writeUTF(value.toString())
            }
        }
    }

    companion object {
        const val MAGIC_NUMBER = 0x504D4144 // "PMAD" - Portrait Metadata
        const val VERSION = 3

        // Type constants for annotation values
        const val TYPE_NULL = 0
        const val TYPE_STRING = 1
        const val TYPE_BOOLEAN = 2
        const val TYPE_INT = 3
        const val TYPE_LONG = 4
        const val TYPE_FLOAT = 5
        const val TYPE_DOUBLE = 6
        const val TYPE_LIST = 7
        const val TYPE_OTHER = 8
    }
}
