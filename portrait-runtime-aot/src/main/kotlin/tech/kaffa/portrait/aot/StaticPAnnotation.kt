package tech.kaffa.portrait.aot

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.aot.meta.PAnnotationEntry

/**
 * AOT implementation of PAnnotation that uses precomputed metadata.
 */
class StaticPAnnotation<T : Annotation>(
    private val annotationEntry: PAnnotationEntry
) : PAnnotation<T>() {

    override val annotationClass: PClass<T> by lazy {
        @Suppress("UNCHECKED_CAST")
        Portrait.forNameOrUnresolved(annotationEntry.annotationClassName) as PClass<T>
    }

    override val simpleName: String get() = annotationEntry.simpleName
    override val qualifiedName: String? get() = annotationEntry.qualifiedName

    private val properties: Map<String, Any?> get() = annotationEntry.properties

    private val proxyInstance: T by lazy {
        annotationClass.createProxy { self, method, args ->
            when (method.name) {
                "annotationType" -> resolveAnnotationType()
                "hashCode" -> computeHashCode()
                "equals" -> equalsProxy(self, args.firstOrNull())
                "toString" -> buildStringRepresentation()
                else -> properties[method.name]
            }
        }
    }

    override fun getValue(propertyName: String): Any? = properties[propertyName]

    override fun get(): T = proxyInstance

    private fun resolveAnnotationType(): Class<out Annotation> {
        @Suppress("UNCHECKED_CAST")
        return Class.forName(annotationEntry.annotationClassName) as Class<out Annotation>
    }

    private fun computeHashCode(): Int {
        if (properties.isEmpty()) return 0
        return properties.entries.fold(0) { acc, (name, value) ->
            acc + (127 * name.hashCode() xor normalizedHash(value))
        }
    }

    private fun equalsProxy(self: Annotation, other: Any?): Boolean {
        if (self === other) return true
        if (other !is Annotation) return false

        val expectedName = qualifiedName ?: annotationEntry.annotationClassName
        val otherName = other.annotationClass.java.name
        if (expectedName != otherName) {
            return false
        }

        return properties.entries.all { (name, expectedValue) ->
            valuesEqual(expectedValue, annotationClass.getMethod(name)?.invoke(other))
        }
    }

    private fun buildStringRepresentation(): String {
        val typeName = qualifiedName ?: annotationEntry.annotationClassName
        if (properties.isEmpty()) return "@$typeName"
        val body = properties.entries.joinToString(", ") { (key, value) ->
            "$key=${formatValue(value)}"
        }
        return "@$typeName($body)"
    }

    private fun normalizedHash(value: Any?): Int = when (val normalized = normalize(value)) {
        null -> 0
        is List<*> -> normalized.fold(1) { acc, item -> 31 * acc + normalizedHash(item) }
        else -> normalized.hashCode()
    }

    private fun valuesEqual(expected: Any?, actual: Any?): Boolean =
        normalize(expected) == normalize(actual)

    private fun formatValue(value: Any?): String = when (val normalized = normalize(value)) {
        null -> "null"
        is String -> "\"$normalized\""
        is List<*> -> normalized.joinToString(prefix = "[", postfix = "]") { formatValue(it) }
        else -> normalized.toString()
    }

    private fun normalize(value: Any?): Any? = when (value) {
        null -> null
        is BooleanArray -> value.toList()
        is ByteArray -> value.toList()
        is CharArray -> value.toList()
        is ShortArray -> value.toList()
        is IntArray -> value.toList()
        is LongArray -> value.toList()
        is FloatArray -> value.toList()
        is DoubleArray -> value.toList()
        is Array<*> -> value.map { normalize(it) }
        else -> value
    }
}
