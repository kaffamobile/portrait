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
    override val interfaces: List<PClass<*>> by lazy {
        listOf(
            Portrait.forNameOrUnresolved(CLONEABLE_CLASS_NAME),
            Portrait.forNameOrUnresolved(SERIALIZABLE_CLASS_NAME)
        )
    }

    override fun createInstance(vararg args: Any?): T {
        throw UnsupportedOperationException("Array instantiation via Portrait not supported")
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        val otherName = other.qualifiedName
        if (!otherName.startsWith("[")) return false
        if (otherName == arrayTypeName) return true

        val thisComponentDescriptor = componentDescriptor ?: return false
        val otherComponentDescriptor = otherName.substring(1)

        if (thisComponentDescriptor == otherComponentDescriptor) return true
        if (isPrimitiveDescriptor(thisComponentDescriptor) || isPrimitiveDescriptor(otherComponentDescriptor)) {
            return false
        }

        if (isJavaLangObjectDescriptor(thisComponentDescriptor)) {
            return !isPrimitiveDescriptor(otherComponentDescriptor)
        }

        if (thisComponentDescriptor.startsWith("[") && otherComponentDescriptor.startsWith("[")) {
            val thisComponent = Portrait.forNameOrUnresolved(thisComponentDescriptor)
            val otherComponent = Portrait.forNameOrUnresolved(otherComponentDescriptor)
            if (Portrait.isUnresolved(thisComponent) || Portrait.isUnresolved(otherComponent)) {
                return false
            }
            return thisComponent.isAssignableFrom(otherComponent)
        }

        val thisComponentName = descriptorToClassName(thisComponentDescriptor) ?: return false
        val otherComponentName = descriptorToClassName(otherComponentDescriptor) ?: return false

        val thisComponent = Portrait.forNameOrUnresolved(thisComponentName)
        val otherComponent = Portrait.forNameOrUnresolved(otherComponentName)
        if (Portrait.isUnresolved(thisComponent) || Portrait.isUnresolved(otherComponent)) {
            return false
        }
        return thisComponent.isAssignableFrom(otherComponent)
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        val otherName = other.qualifiedName
        return otherName == OBJECT_CLASS_NAME ||
                otherName == CLONEABLE_CLASS_NAME ||
                otherName == SERIALIZABLE_CLASS_NAME
    }

    override val annotations: List<PAnnotation> = emptyList()
    override fun getAnnotation(annotationClass: PClass<*>): PAnnotation? = null
    override fun hasAnnotation(annotationClass: PClass<*>): Boolean = false

    override val constructors: List<PConstructor<T>> = emptyList()
    override fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>? = null

    override val methods: List<PMethod> = emptyList()
    override fun getMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? = null

    override val fields: List<PField> = emptyList()
    override fun getField(name: String): PField? = null

    override fun createProxy(handler: ProxyHandler<T>): T {
        throw UnsupportedOperationException("Cannot create proxy for array type $simpleName")
    }

    override fun toString(): String = "WellKnownArrayPClass($simpleName)"

    companion object {
        /** Canonical type name for `java.lang.Object`. */
        private const val OBJECT_CLASS_NAME: String = "java.lang.Object"
        private const val CLONEABLE_CLASS_NAME: String = "java.lang.Cloneable"
        private const val SERIALIZABLE_CLASS_NAME: String = "java.io.Serializable"

        private val PRIMITIVE_DESCRIPTORS = setOf("Z", "B", "C", "S", "I", "J", "F", "D")

        private fun isPrimitiveDescriptor(descriptor: String): Boolean {
            return PRIMITIVE_DESCRIPTORS.contains(descriptor)
        }

        private fun isJavaLangObjectDescriptor(descriptor: String): Boolean {
            return descriptor == "Ljava.lang.Object;" || descriptor == "Ljava/lang/Object;"
        }

        private fun descriptorToClassName(descriptor: String): String? {
            return when {
                descriptor.startsWith("[") -> descriptor
                descriptor == "Z" -> "boolean"
                descriptor == "B" -> "byte"
                descriptor == "C" -> "char"
                descriptor == "S" -> "short"
                descriptor == "I" -> "int"
                descriptor == "J" -> "long"
                descriptor == "F" -> "float"
                descriptor == "D" -> "double"
                descriptor == "V" -> null
                descriptor.startsWith("L") && descriptor.endsWith(";") -> descriptor.substring(1, descriptor.length - 1).replace("/", ".")
                else -> null
            }
        }
    }

    private val componentDescriptor: String? by lazy {
        if (!arrayTypeName.startsWith("[")) null else arrayTypeName.substring(1)
    }
}
