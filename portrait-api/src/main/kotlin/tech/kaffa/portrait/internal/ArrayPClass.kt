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
        val (component, dims) = splitArray(arrayTypeName)
        primitiveMap.getOrElse(component) {
            component
                .removeSurrounding("L", ";")
                .replace("/", ".")
                .substringAfterLast('.')
        }  + "[]".repeat(dims)
    }

    override val qualifiedName: String = arrayTypeName

    override val isAbstract: Boolean = false
    override val isSealed: Boolean = false
    override val isData: Boolean = false
    override val isCompanion: Boolean = false
    override val isEnum: Boolean = false
    override val objectInstance: T? = null
    override val enumConstants: Array<T>? = null
    override val superclass: PClass<*> by lazy {
        Portrait.forNameOrUnresolved(OBJECT_CLASS_NAME)
    }
    override val interfaces: List<PClass<*>> by lazy {
        listOf(
            Portrait.forNameOrUnresolved(CLONEABLE_CLASS_NAME),
            Portrait.forNameOrUnresolved(SERIALIZABLE_CLASS_NAME)
        )
    }

    override fun newInstance(vararg args: Any?): T {
        throw UnsupportedOperationException("Array instantiation via Portrait not supported")
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        if (this === other) return true
        val otherName = other.qualifiedName
        if (!otherName.startsWith("[")) return false
        return isArrayAssignableFrom(arrayTypeName, otherName)
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        if (this === other) return true
        if (qualifiedName == other.qualifiedName) return true

        val otherName = other.qualifiedName
        return when {
            otherName == OBJECT_CLASS_NAME
                || otherName == CLONEABLE_CLASS_NAME
                || otherName == SERIALIZABLE_CLASS_NAME -> true
            !otherName.startsWith("[") -> false
            else -> isArrayAssignableFrom(otherName, arrayTypeName)
        }
    }

    override val annotations: List<PAnnotation<*>> = emptyList()
    override fun <A : Annotation> getAnnotation(annotationClass: PClass<A>): PAnnotation<A>? = null
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

    override fun toString(): String {
        val displayName = descriptorToReadableName(arrayTypeName)
        return buildString {
            append("PClass[array](")

            val (component, dims) = splitArray(arrayTypeName)
            append(
                primitiveMap.getOrElse(component) {
                    component
                        .removeSurrounding("L", ";")
                        .replace("/", ".")
                }
            )
            repeat(dims) { append("[]") }
            append(displayName)
            append(")")
        }
    }

    companion object {
        private const val OBJECT_CLASS_NAME: String = "java.lang.Object"
        private const val CLONEABLE_CLASS_NAME: String = "java.lang.Cloneable"
        private const val SERIALIZABLE_CLASS_NAME: String = "java.io.Serializable"

        private val primitiveMap = mapOf(
            "Z" to "boolean",
            "B" to "byte",
            "C" to "char",
            "S" to "short",
            "I" to "int",
            "J" to "long",
            "F" to "float",
            "D" to "double",
        )

        private data class ArraySplit(val component: String, val dimensions: Int)

        /** Strips leading '[' and returns the non-array component + dimension count. */
        private fun splitArray(descriptor: String): ArraySplit {
            var dims = 0
            var s = descriptor
            while (s.startsWith("[")) {
                dims++
                s = s.substring(1)
            }
            return ArraySplit(s, dims)
        }

        /** Fully-qualified base name (or primitive keyword) from a non-array component descriptor. */
        private fun qualifiedNameFromComponent(component: String): String? = when {
            component in primitiveMap -> primitiveMap.getValue(component)
            component == "V" -> null
            component.startsWith("L") && component.endsWith(";") ->
                component.substring(1, component.length - 1).replace("/", ".")
            component.startsWith("[") -> component // already an array component; keep as-is
            else -> null
        }

        private fun isJavaLangObjectDescriptor(descriptor: String): Boolean {
            return descriptor == "Ljava.lang.Object;" || descriptor == "Ljava/lang/Object;"
        }

        private fun descriptorToReadableName(descriptor: String): String {
            val (component, dims) = splitArray(descriptor)
            val baseName = when {
                component in primitiveMap -> primitiveMap.getValue(component)
                component == "V" -> "void"
                component.startsWith("L") && component.endsWith(";") ->
                    component.substring(1, component.length - 1).replace("/", ".")
                else -> component
            }

            return buildString {
                append(baseName)
                repeat(dims) { append("[]") }
            }
        }

        private fun isArrayAssignableFrom(targetDescriptor: String, sourceDescriptor: String): Boolean {
            // Both must be array descriptors like "[I" or "[[Ljava/lang/String;"
            if (!targetDescriptor.startsWith("[")) return false
            if (!sourceDescriptor.startsWith("[")) return false

            // Identical array descriptors are trivially assignable
            if (targetDescriptor == sourceDescriptor) return true

            // Strip one leading '[' to look at the immediate component
            val targetComponent = targetDescriptor.substring(1)
            val sourceComponent = sourceDescriptor.substring(1)

            // Same immediate component → assignable (e.g., "[I" vs "[I", or "[[Ljava/lang/String;" vs same)
            if (targetComponent == sourceComponent) return true

            // If either component is a primitive, different primitives are never assignable
            if (targetComponent in primitiveMap || sourceComponent in primitiveMap) {
                return false
            }

            // Object[] can hold any non-primitive component (including arrays of reference types)
            if (isJavaLangObjectDescriptor(targetComponent)) {
                return sourceComponent !in primitiveMap
            }

            // If both components are arrays, recurse on the sub-arrays
            val targetIsArray = targetComponent.startsWith("[")
            val sourceIsArray = sourceComponent.startsWith("[")
            if (targetIsArray && sourceIsArray) {
                return isArrayAssignableFrom(targetComponent, sourceComponent)
            }
            // One is array and the other isn't (e.g., String[][] vs String[]) → not assignable
            if (targetIsArray || sourceIsArray) return false

            // Both components are reference types like "Ljava/lang/Number;" → resolve and check
            val targetClassName = qualifiedNameFromComponent(targetComponent) ?: return false
            val sourceClassName = qualifiedNameFromComponent(sourceComponent) ?: return false

            val targetClass = Portrait.forNameOrUnresolved(targetClassName)
            val sourceClass = Portrait.forNameOrUnresolved(sourceClassName)

            if (Portrait.isUnresolved(targetClass) || Portrait.isUnresolved(sourceClass)) return false
            return targetClass.isAssignableFrom(sourceClass)
        }
    }
}


