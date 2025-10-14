package tech.kaffa.portrait.internal

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PConstructor
import tech.kaffa.portrait.PField
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.proxy.ProxyHandler

/**
 * PClass implementation for array types of well-known classes.
 */
internal class ArrayPClass<T : Any>(private val arrayTypeName: String) : PClass<T>() {
    override val simpleName: String by lazy {
        // Convert array notation to simple name like "[I" -> "int[]"
        var dimensions = 0
        var name = arrayTypeName
        while (name.startsWith("[")) {
            dimensions++
            name = name.substring(1)
        }
        val baseName = when {
            name == "Z" -> "boolean"
            name == "B" -> "byte"
            name == "C" -> "char"
            name == "S" -> "short"
            name == "I" -> "int"
            name == "J" -> "long"
            name == "F" -> "float"
            name == "D" -> "double"
            name == "V" -> "void"
            name.startsWith("L") && name.endsWith(";") -> {
                val fullName = name.substring(1, name.length - 1).replace("/", ".")
                fullName.substringAfterLast('.')
            }

            else -> name
        }
        baseName + "[]".repeat(dimensions)
    }

    override val qualifiedName: String = arrayTypeName

    override val isAbstract: Boolean = false
    override val isSealed: Boolean = false
    override val isData: Boolean = false
    override val isCompanion: Boolean = false
    override val objectInstance: T? = null
    override val superclass: PClass<*> by lazy {
        Portrait.forNameOrUnresolved(OBJECT_CLASS_NAME)
    }
    override val interfaces: List<PClass<*>> = emptyList()

    override fun createInstance(vararg args: Any?): T {
        throw UnsupportedOperationException("Array instantiation via Portrait not supported")
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        return other.qualifiedName == this.qualifiedName
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        return other.qualifiedName == OBJECT_CLASS_NAME
    }

    override val annotations: List<PAnnotation> = emptyList()
    override fun getAnnotation(annotationClass: PClass<*>): PAnnotation? = null
    override fun hasAnnotation(annotationClass: PClass<*>): Boolean = false

    override val constructors: List<PConstructor<T>> = emptyList()
    override fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>? = null

    override val declaredMethods: List<PMethod> = emptyList()
    override fun getDeclaredMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? = null

    override val declaredFields: List<PField> = emptyList()
    override fun getDeclaredField(name: String): PField? = null

    override fun createProxy(handler: ProxyHandler<T>): T {
        throw UnsupportedOperationException("Cannot create proxy for array type $simpleName")
    }

    override fun toString(): String = "WellKnownArrayPClass($simpleName)"

    companion object {
        /** Canonical type name for `java.lang.Object`. */
        private const val OBJECT_CLASS_NAME: String = "java.lang.Object"
    }
}
