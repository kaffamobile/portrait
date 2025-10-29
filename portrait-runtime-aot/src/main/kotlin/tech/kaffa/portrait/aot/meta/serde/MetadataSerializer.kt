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
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.Base64

class MetadataSerializer {

    fun serialize(clazz: PClassEntry): String {
        val stringPool = StringPool()
        collectClassStrings(stringPool, clazz)

        val strings = stringPool.getStrings()
        val stringIndexWidth = IntWidth.forUpperBound(strings.size - 1)

        val output = ByteArrayOutputStream()
        val data = DataOutputStream(output)

        data.writeShort(VERSION)
        data.writeByte(stringIndexWidth.id)
        stringIndexWidth.write(data, strings.size)

        strings.forEach { data.writeUTF(it) }

        val context = EncodingContext(stringPool, stringIndexWidth)
        writeClass(data, clazz, context)

        return Base64.getEncoder().encodeToString(output.toByteArray())
    }

    private fun collectClassStrings(stringPool: StringPool, clazz: PClassEntry) {
        stringPool.intern(clazz.simpleName)
        stringPool.intern(clazz.qualifiedName)
        stringPool.intern(clazz.javaClassName)
        clazz.superclassName?.let { stringPool.intern(it) }
        clazz.interfaceNames.forEach { stringPool.intern(it) }

        clazz.annotations.forEach { collectAnnotationStrings(stringPool, it) }
        clazz.constructors.forEach { collectConstructorStrings(stringPool, it) }
        clazz.declaredMethods.forEach { collectMethodStrings(stringPool, it) }
        clazz.declaredFields.forEach { collectFieldStrings(stringPool, it) }
        clazz.proxyMethods.forEach { collectMethodStrings(stringPool, it) }
    }

    private fun collectConstructorStrings(stringPool: StringPool, constructor: PConstructorEntry) {
        stringPool.intern(constructor.declaringClassName)
        constructor.parameterTypeNames.forEach { stringPool.intern(it) }
        constructor.annotations.forEach { collectAnnotationStrings(stringPool, it) }
    }

    private fun collectFieldStrings(stringPool: StringPool, field: PFieldEntry) {
        stringPool.intern(field.name)
        stringPool.intern(field.typeName)
        stringPool.intern(field.declaringClassName)
        field.annotations.forEach { collectAnnotationStrings(stringPool, it) }
    }

    private fun collectMethodStrings(stringPool: StringPool, method: PMethodEntry) {
        stringPool.intern(method.name)
        method.parameterTypeNames.forEach { stringPool.intern(it) }
        stringPool.intern(method.returnTypeName)
        collectTypeStrings(stringPool, method.genericReturnType)
        stringPool.intern(method.declaringClassName)
        method.annotations.forEach { collectAnnotationStrings(stringPool, it) }
        method.parameterAnnotations.forEach { annotations ->
            annotations.forEach { collectAnnotationStrings(stringPool, it) }
        }
    }

    private fun collectTypeStrings(stringPool: StringPool, type: PTypeEntry) {
        when (type) {
            is PClassTypeEntry -> stringPool.intern(type.className)
            is PParameterizedTypeEntry -> {
                stringPool.intern(type.rawTypeName)
                type.ownerType?.let { collectTypeStrings(stringPool, it) }
                type.arguments.forEach { collectTypeStrings(stringPool, it) }
            }
            is PTypeVariableEntry -> {
                stringPool.intern(type.name)
                type.bounds.forEach { collectTypeStrings(stringPool, it) }
            }
            is PWildcardTypeEntry -> {
                type.upperBounds.forEach { collectTypeStrings(stringPool, it) }
                type.lowerBounds.forEach { collectTypeStrings(stringPool, it) }
            }
            is PGenericArrayTypeEntry -> collectTypeStrings(stringPool, type.componentType)
        }
    }

    private fun collectAnnotationStrings(stringPool: StringPool, annotation: PAnnotationEntry) {
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

    private fun writeClass(
        data: DataOutputStream,
        clazz: PClassEntry,
        context: EncodingContext
    ) {
        context.writeIndex(data, clazz.simpleName)
        context.writeIndex(data, clazz.qualifiedName)

        val collectionWidth = IntWidth.forUpperBound(
            maxOf(
                clazz.interfaceNames.size,
                clazz.constructors.size,
                clazz.declaredMethods.size,
                clazz.declaredFields.size
            )
        )

        val flags = buildClassFlags(clazz, collectionWidth)
        data.writeByte(flags)

        context.writeIndex(data, clazz.javaClassName)
        if (clazz.superclassName != null) {
            context.writeIndex(data, clazz.superclassName)
        }

        collectionWidth.write(data, clazz.interfaceNames.size)
        clazz.interfaceNames.forEach { context.writeIndex(data, it) }

        writeAnnotations(data, clazz.annotations, context)

        collectionWidth.write(data, clazz.constructors.size)
        clazz.constructors.forEach { writeConstructor(data, it, context) }

        collectionWidth.write(data, clazz.declaredMethods.size)
        clazz.declaredMethods.forEach { writeMethod(data, it, context) }

        collectionWidth.write(data, clazz.declaredFields.size)
        clazz.declaredFields.forEach { writeField(data, it, context) }

        if (clazz.proxyMethods.isNotEmpty()) {
            data.writeInt(clazz.proxyMethods.size)
            clazz.proxyMethods.forEach { writeMethod(data, it, context) }
        }

    }

    private fun writeConstructor(
        data: DataOutputStream,
        constructor: PConstructorEntry,
        context: EncodingContext
    ) {
        context.writeIndex(data, constructor.declaringClassName)

        val parameterWidth = IntWidth.forUpperBound(constructor.parameterTypeNames.size)
        data.writeByte(parameterWidth.id)
        parameterWidth.write(data, constructor.parameterTypeNames.size)
        constructor.parameterTypeNames.forEach { context.writeIndex(data, it) }

        writeAnnotations(data, constructor.annotations, context)
    }

    private fun writeField(
        data: DataOutputStream,
        field: PFieldEntry,
        context: EncodingContext
    ) {
        context.writeIndex(data, field.name)
        context.writeIndex(data, field.typeName)
        context.writeIndex(data, field.declaringClassName)
        data.writeByte(buildFieldFlags(field))

        writeAnnotations(data, field.annotations, context)
    }

    private fun writeMethod(
        data: DataOutputStream,
        method: PMethodEntry,
        context: EncodingContext
    ) {
        context.writeIndex(data, method.name)
        context.writeIndex(data, method.returnTypeName)
        writeType(data, method.genericReturnType, context)
        context.writeIndex(data, method.declaringClassName)

        val parameterWidth = IntWidth.forUpperBound(
            maxOf(
                method.parameterTypeNames.size,
                method.parameterAnnotations.size
            )
        )
        data.writeByte(buildMethodFlags(method, parameterWidth))

        parameterWidth.write(data, method.parameterTypeNames.size)
        method.parameterTypeNames.forEach { context.writeIndex(data, it) }

        writeAnnotations(data, method.annotations, context)

        parameterWidth.write(data, method.parameterAnnotations.size)
        method.parameterAnnotations.forEach { annotations ->
            writeAnnotations(data, annotations, context)
        }
    }

    private fun writeType(
        data: DataOutputStream,
        type: PTypeEntry,
        context: EncodingContext
    ) {
        when (type) {
            is PClassTypeEntry -> {
                data.writeByte(GENERIC_TYPE_CLASS)
                context.writeIndex(data, type.className)
            }
            is PParameterizedTypeEntry -> {
                data.writeByte(GENERIC_TYPE_PARAMETERIZED)
                context.writeIndex(data, type.rawTypeName)
                data.writeBoolean(type.ownerType != null)
                type.ownerType?.let { writeType(data, it, context) }

                val argumentWidth = IntWidth.forUpperBound(type.arguments.size)
                data.writeByte(argumentWidth.id)
                argumentWidth.write(data, type.arguments.size)
                type.arguments.forEach { writeType(data, it, context) }
            }
            is PTypeVariableEntry -> {
                data.writeByte(GENERIC_TYPE_VARIABLE)
                context.writeIndex(data, type.name)
                val boundsWidth = IntWidth.forUpperBound(type.bounds.size)
                data.writeByte(boundsWidth.id)
                boundsWidth.write(data, type.bounds.size)
                type.bounds.forEach { writeType(data, it, context) }
            }
            is PWildcardTypeEntry -> {
                data.writeByte(GENERIC_TYPE_WILDCARD)

                val upperWidth = IntWidth.forUpperBound(type.upperBounds.size)
                data.writeByte(upperWidth.id)
                upperWidth.write(data, type.upperBounds.size)
                type.upperBounds.forEach { writeType(data, it, context) }

                val lowerWidth = IntWidth.forUpperBound(type.lowerBounds.size)
                data.writeByte(lowerWidth.id)
                lowerWidth.write(data, type.lowerBounds.size)
                type.lowerBounds.forEach { writeType(data, it, context) }
            }
            is PGenericArrayTypeEntry -> {
                data.writeByte(GENERIC_TYPE_GENERIC_ARRAY)
                writeType(data, type.componentType, context)
            }
        }
    }

    private fun writeAnnotations(
        data: DataOutputStream,
        annotations: List<PAnnotationEntry>,
        context: EncodingContext
    ) {
        data.writeInt(annotations.size)
        annotations.forEach { writeAnnotation(data, it, context) }
    }

    private fun writeAnnotation(
        data: DataOutputStream,
        annotation: PAnnotationEntry,
        context: EncodingContext
    ) {
        context.writeIndex(data, annotation.annotationClassName)
        context.writeIndex(data, annotation.simpleName)

        val hasQualifiedName = annotation.qualifiedName != null
        data.writeBoolean(hasQualifiedName)
        if (hasQualifiedName) {
            context.writeIndex(data, annotation.qualifiedName!!)
        }

        data.writeInt(annotation.properties.size)
        annotation.properties.forEach { (key, value) ->
            context.writeIndex(data, key)
            writeAnnotationValue(data, context, value)
        }
    }

    private fun writeAnnotationValue(
        data: DataOutputStream,
        context: EncodingContext,
        value: Any?
    ) {
        when (value) {
            null -> data.writeByte(TYPE_NULL)
            is String -> {
                data.writeByte(TYPE_STRING)
                context.writeIndex(data, value)
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
            is List<*> -> writeAnnotationList(data, context, value)
            else -> {
                data.writeByte(TYPE_OTHER)
                data.writeUTF(value.toString())
            }
        }
    }

    private fun writeAnnotationList(
        data: DataOutputStream,
        context: EncodingContext,
        values: List<*>
    ) {
        val (type, width) = listEncodingForSize(values.size)
        data.writeByte(type)
        width.write(data, values.size)
        values.forEach { item ->
            writeAnnotationValue(data, context, item)
        }
    }

    private fun listEncodingForSize(size: Int): Pair<Int, IntWidth> {
        val width = IntWidth.forUpperBound(size)
        val type = when (width) {
            IntWidth.U8 -> TYPE_LIST_U8
            IntWidth.U16 -> TYPE_LIST_U16
            IntWidth.U24 -> TYPE_LIST_U24
            IntWidth.U32 -> TYPE_LIST_INT
        }
        return type to width
    }

    private data class EncodingContext(
        val stringPool: StringPool,
        val stringWidth: IntWidth
    ) {
        fun writeIndex(data: DataOutputStream, value: String) {
            stringWidth.write(data, stringPool.indexOf(value))
        }
    }

    companion object {
        const val VERSION = 7

        const val GENERIC_TYPE_CLASS = 0
        const val GENERIC_TYPE_PARAMETERIZED = 1
        const val GENERIC_TYPE_VARIABLE = 2
        const val GENERIC_TYPE_WILDCARD = 3
        const val GENERIC_TYPE_GENERIC_ARRAY = 4

        const val TYPE_NULL = 0
        const val TYPE_STRING = 1
        const val TYPE_BOOLEAN = 2
        const val TYPE_INT = 3
        const val TYPE_LONG = 4
        const val TYPE_FLOAT = 5
        const val TYPE_DOUBLE = 6
        const val TYPE_LIST_U8 = 7
        const val TYPE_LIST_U16 = 8
        const val TYPE_LIST_U24 = 9
        const val TYPE_LIST_INT = 10
        const val TYPE_OTHER = 11
    }
}
