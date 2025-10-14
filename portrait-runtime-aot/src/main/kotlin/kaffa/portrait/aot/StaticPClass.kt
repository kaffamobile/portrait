package kaffa.portrait.aot

import kaffa.portrait.PAnnotation
import kaffa.portrait.PClass
import kaffa.portrait.PConstructor
import kaffa.portrait.PField
import kaffa.portrait.PMethod
import kaffa.portrait.Portrait
import kaffa.portrait.proxy.ProxyHandler
import kaffa.portrait.aot.meta.PClassEntry
import kaffa.portrait.aot.meta.serde.MetadataDeserializer
import kaffa.portrait.internal.BoxedPrimitives

/**
 * AOT implementation of PClass that uses precomputed metadata and StaticPortrait for reflection operations.
 *
 * This implementation provides reflection capabilities at compile time using generated metadata
 * and delegates actual operations to the associated StaticPortrait instance.
 *
 * @param T The type represented by this PClass
 * @param staticPortrait The StaticPortrait instance for this class
 */
class StaticPClass<T : Any>(
    private val staticPortrait: StaticPortrait<T>
) : PClass<T>(), ProxyMethodIndexer {

    private val classEntry: PClassEntry by lazy {
        MetadataDeserializer().deserialize(staticPortrait.metadata)
    }

    override val simpleName: String get() = classEntry.simpleName
    override val qualifiedName: String? get() = classEntry.qualifiedName
    override val isAbstract: Boolean get() = classEntry.isAbstract
    override val isSealed: Boolean get() = classEntry.isSealed
    override val isData: Boolean get() = classEntry.isData
    override val isCompanion: Boolean get() = classEntry.isCompanion

    override val objectInstance: T?
        get() =
            if (classEntry.isObject) {
                try {
                    staticPortrait.objectInstance
                } catch (e: UnsupportedOperationException) {
                    null
                }
            } else {
                null
            }

    override val superclass: PClass<*>? by lazy {
        val superclassName = classEntry.superclassName ?: return@lazy null
        Portrait.forNameOrUnresolved(superclassName)
    }

    override val interfaces: List<PClass<*>> by lazy {
        classEntry.interfaceNames.map { interfaceName ->
            Portrait.forNameOrUnresolved(interfaceName)
        }
    }

    override fun createInstance(vararg args: Any?): T {
        // Find the constructor that matches the arguments
        val constructor = constructors.find { constructor ->
            constructor.parameterTypes.size == args.size &&
                    constructor.parameterTypes.zip(args.toList()).all { (paramType, arg) ->
                        arg == null || (paramType.qualifiedName == arg::class.qualifiedName)
                    }
        } ?: throw IllegalArgumentException("No matching constructor found for arguments: ${args.contentToString()}")

        return constructor.call(*args)
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        val thisQualifiedName = qualifiedName
        if (thisQualifiedName == other.qualifiedName) {
            return true
        }

        val wrapperPrimitive = thisQualifiedName?.let { BoxedPrimitives.unboxing[it] }
        if (wrapperPrimitive != null && other.isPrimitive && other.qualifiedName == wrapperPrimitive) {
            return true
        }

        return other.qualifiedName in classEntry.interfaceNames ||
                other.qualifiedName == classEntry.superclassName
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        val otherName = other.qualifiedName ?: return false
        if (qualifiedName == otherName) return false

        var current: PClass<*>? = superclass
        while (current != null) {
            if (current.qualifiedName == otherName) {
                return true
            }
            current = current.superclass
        }

        return interfaces.any { it.qualifiedName == otherName }
    }

    override val annotations: List<PAnnotation> by lazy {
        classEntry.annotations.map { StaticPAnnotation(it) }
    }

    override fun getAnnotation(annotationClass: PClass<*>): PAnnotation? =
        annotations.find { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override fun hasAnnotation(annotationClass: PClass<*>): Boolean =
        annotations.any { it.annotationClass.qualifiedName == annotationClass.qualifiedName }

    override val constructors: List<PConstructor<T>> by lazy {
        classEntry.constructors.withIndex().map { (i, constructorEntry) ->
            StaticPConstructor(constructorEntry, i, this, staticPortrait)
        }
    }

    override fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>? =
        constructors.find { constructor ->
            constructor.parameterTypes.size == parameterTypes.size &&
                    constructor.parameterTypes.zip(parameterTypes.toList())
                        .all { (constructorParamType, requestedParamType) ->
                            constructorParamType.qualifiedName == requestedParamType.qualifiedName
                        }
        }

    override val declaredMethods: List<PMethod> by lazy {
        classEntry.declaredMethods.withIndex().map { (i, methodEntry) ->
            StaticPMethod(methodEntry, i, staticPortrait)
        }
    }

    override fun getDeclaredMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? {
        val methodsWithName = declaredMethods.filter { it.name == name }

        return when {
            parameterTypes.isEmpty() -> {
                // If no parameter types specified, return the method only if there's exactly one with that name
                if (methodsWithName.size == 1) methodsWithName.first() else null
            }

            else -> {
                // If parameter types specified, find exact match
                methodsWithName.find { method ->
                    method.parameterTypes.size == parameterTypes.size &&
                            method.parameterTypes.zip(parameterTypes.toList())
                                .all { (methodParamType, requestedParamType) ->
                                    methodParamType.qualifiedName == requestedParamType.qualifiedName
                                }
                }
            }
        }
    }

    override val declaredFields: List<PField> by lazy {
        classEntry.declaredFields.withIndex().map { (i, fieldEntry) ->
            StaticPField(fieldEntry, i, staticPortrait)
        }
    }

    override fun getDeclaredField(name: String): PField? =
        declaredFields.find { it.name == name }

    override fun createProxy(handler: ProxyHandler<T>): T {
        return staticPortrait.createProxy(this, handler)
    }

    override fun method(index: Int): PMethod {
        return StaticPMethod(classEntry.proxyMethods[index], index, staticPortrait)
    }
}
