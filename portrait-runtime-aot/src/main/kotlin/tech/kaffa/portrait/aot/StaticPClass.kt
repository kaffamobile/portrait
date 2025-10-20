package tech.kaffa.portrait.aot

import tech.kaffa.portrait.PAnnotation
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.PConstructor
import tech.kaffa.portrait.PField
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.serde.MetadataDeserializer
import tech.kaffa.portrait.internal.BoxedPrimitives
import tech.kaffa.portrait.proxy.ProxyHandler

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
    override val qualifiedName: String get() = classEntry.qualifiedName
    override val isAbstract: Boolean get() = classEntry.isAbstract
    override val isSealed: Boolean get() = classEntry.isSealed
    override val isData: Boolean get() = classEntry.isData
    override val isCompanion: Boolean get() = classEntry.isCompanion
    override val isEnum: Boolean get() = classEntry.isEnum
    override val enumConstants: Array<T>?
        get() =
            if (classEntry.isEnum) {
                try {
                    staticPortrait.enumConstants
                } catch (e: UnsupportedOperationException) {
                    null
                }
            } else {
                null
            }

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
        // Find the constructor that matches the arguments using Portrait's
        // assignability (handles boxing, subtyping, arrays). Accept null for
        // reference types and reject for primitive parameters.
        val ctor = constructors.find { ctor ->
            if (ctor.parameterTypes.size != args.size) return@find false
            ctor.parameterTypes.zip(args.asList()).all { (paramType, arg) ->
                if (arg == null) {
                    !paramType.isPrimitive
                } else {
                    val argType = Portrait.from(arg)
                    paramType.isAssignableFrom(argType)
                }
            }
        } ?: throw IllegalArgumentException(
            "No matching constructor found for arguments: ${args.contentToString()}"
        )

        return ctor.call(*args)
    }

    override fun isAssignableFrom(other: PClass<*>): Boolean {
        val thisQualifiedName = qualifiedName
        if (thisQualifiedName == other.qualifiedName) return true

        // Primitive boxing bridge: wrapper assignable from primitive
        val wrapperPrimitive = BoxedPrimitives.unboxing[thisQualifiedName]
        if (wrapperPrimitive != null && other.isPrimitive && other.qualifiedName == wrapperPrimitive) return true

        // General case: if 'other' is a subclass/implements this
        return other.isSubclassOf(this)
    }

    override fun isSubclassOf(other: PClass<*>): Boolean {
        val otherName = other.qualifiedName
        if (qualifiedName == otherName) return false

        // Walk superclass chain
        var current: PClass<*>? = superclass
        while (current != null) {
            if (current.qualifiedName == otherName) return true
            current = current.superclass
        }

        // Check interfaces (including their super-interfaces)
        return interfaces.any { it.qualifiedName == otherName || it.isSubclassOf(other) }
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

    override val methods: List<PMethod> by lazy {
        classEntry.declaredMethods.withIndex().map { (i, methodEntry) ->
            StaticPMethod(methodEntry, i, staticPortrait)
        }
    }

    override fun getMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? {
        val methodsWithName = methods.filter { it.name == name }

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

    override val fields: List<PField> by lazy {
        classEntry.declaredFields.withIndex().map { (i, fieldEntry) ->
            StaticPField(fieldEntry, i, staticPortrait)
        }
    }

    override fun getField(name: String): PField? =
        fields.find { it.name == name }

    override fun createProxy(handler: ProxyHandler<T>): T {
        return staticPortrait.createProxy(this, handler)
    }

    override fun method(index: Int): PMethod {
        return StaticPMethod(classEntry.proxyMethods[index], index, staticPortrait)
    }
}
