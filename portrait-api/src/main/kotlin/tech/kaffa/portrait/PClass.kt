package tech.kaffa.portrait

import tech.kaffa.portrait.proxy.ProxyHandler

/**
 * Describes a runtime type that Portrait can introspect without exposing the host reflection API.
 *
 * `PClass` instances originate from `PortraitProvider`s and may wrap a JVM `Class`, generated
 * `*$Portrait` metadata, or another platform-specific descriptor. The public surface is identical
 * across environments so application code can query types in restricted runtimes such as GraalVM
 * native images or TeaVM where Java reflection is unavailable.
 *
 * Implementations must be thread-safe, defer any classpath or metadata loading until a property is
 * accessed, and are encouraged to cache derived members using the shared sentinel strategy to avoid
 * recursive lookups. Equality and hashing use the qualified name to allow providers to share
 * instances safely across threads.
 *
 * @param T runtime type represented by this descriptor
 */
abstract class PClass<T : Any> {

    /**
     * Kotlin-style simple name without the package qualifier.
     *
     * Implementations must return the declaration name present in metadata even for generated
     * classes. Local or anonymous classes are rejected earlier in the pipeline and must not surface
     * as `PClass` instances.
     */
    abstract val simpleName: String

    /**
     * Fully qualified binary name for the represented type.
     *
     * Providers must use JVM binary naming (with `$` for inner classes) to preserve interoperability
     * between code-generated and runtime descriptors. Implementations must never expose descriptors
     * for types without a stable binary name (for example local or anonymous classes); APIs such as
     * [Portrait.from] enforce this contract by rejecting those types before lookup.
     */
    abstract val qualifiedName: String

    /**
     * True when the underlying declaration cannot be instantiated directly.
     *
     * Providers must mirror JVM/Kotlin semantics and mark abstract classes, interfaces, and
     * annotation types as abstract.
     */
    abstract val isAbstract: Boolean

    /**
     * True when Kotlin metadata marks this class as sealed.
     *
     * Implementations must look at the Kotlin metadata flag even when executing outside the JVM.
     */
    abstract val isSealed: Boolean

    /**
     * True when Kotlin metadata marks this class as a data class.
     *
     * Providers should fall back to `false` when Kotlin metadata is absent.
     */
    abstract val isData: Boolean

    /**
     * True when this descriptor represents a Kotlin companion object.
     *
     * Providers must not report `true` for regular nested objects.
     */
    abstract val isCompanion: Boolean

    /**
     * The singleton instance if the type is a Kotlin object; null otherwise.
     *
     * Implementations may compute this lazily but must guarantee idempotence. Non-object instances
     * must return `null`.
     */
    abstract val objectInstance: T?

    /**
     * Direct superclass descriptor, or null for roots (`Any`/`Object`) and interfaces.
     *
     * Providers may resolve this lazily; cyclic lookups must be guarded via the shared sentinel.
     */
    abstract val superclass: PClass<*>?

    /**
     * Interfaces declared directly on this type, resolved lazily by providers.
     *
     * Implementations must preserve declaration order for deterministic code generation.
     */
    abstract val interfaces: List<PClass<*>>

    /**
     * True when this descriptor represents a JVM primitive or platform equivalent.
     *
     * The default implementation returns `false` so objects and boxed primitives do not require
     * special handling. Implementations that override this property must also override
     * [isAssignableFrom] to honour primitive widening rules and prevent nullable values from being
     * treated as compatible.
     */
    open val isPrimitive = false

    /**
     * All annotations declared on this type; entries are provider-specific but never inherited.
     *
     * Providers must preserve repeatable annotation multiplicity and the declaration order given by
     * the platform metadata.
     */
    abstract val annotations: List<PAnnotation>

    /**
     * Constructors declared by the type. Order is implementation-defined but must be stable within
     * a single provider.
     *
     * Implementations must reference only declared constructors (no synthetic/inherited ones) and
     * may delay materialisation until requested. Only public constructors should be exposed.
     */
    abstract val constructors: List<PConstructor<T>>

    /**
     * Methods declared directly on the type (inherited members are excluded).
     *
     * Providers are responsible for including synthetic accessors when required for invocation,
     * while keeping inherited members out of the list. Only public methods should be exposed.
     */
    abstract val methods: List<PMethod>

    /**
     * Fields declared directly on the type (inherited members are excluded).
     *
     * Implementations must expose companion backing fields and Kotlin properties marked `@JvmField`
     * when present on the target platform. Only public fields should be exposed.
     */
    abstract val fields: List<PField>

    /**
     * Convenience helper that locates a matching constructor and invokes it with the supplied
     * arguments. Matching proceeds in three phases:
     *
     * 1. Prefer the zero-argument constructor when no arguments are given.
     * 2. Attempt an exact match using parameter descriptors derived from argument runtime types.
     * 3. Fallback to a linear search that accepts assignable arguments (including `null` for
     *    non-primitive parameters).
     *
     * Implementations may override this method to provide faster matching or to surface platform
     * specific diagnostics. Overrides must honour the selection order described above and rely on
     * the same public-constructor contract enforced by [constructors].
     *
     * @param args arguments forwarded to the constructor
     * @return a newly constructed instance
     * @throws IllegalArgumentException when no constructor matches the provided arguments
     * @throws RuntimeException if constructor invocation fails for provider-specific reasons
     */
    open fun createInstance(vararg args: Any?): T {
        if (args.isEmpty()) {
            val constructor = getConstructor()
                ?: throw IllegalArgumentException("No zero-argument constructor for $qualifiedName")
            return constructor.call()
        }

        val argumentTypes = args.map { it?.portrait }

        if (argumentTypes.all { it != null }) {
            val parameterTypes = argumentTypes.filterNotNull().toTypedArray()
            val constructor = getConstructor(*parameterTypes)
            if (constructor != null) {
                return constructor.call(*args)
            }
        }

        val matchingConstructor = constructors.firstOrNull { constructor ->
            if (constructor.parameterTypes.size != args.size) return@firstOrNull false
            constructor.parameterTypes.zip(args).all { (parameterType, value) ->
                if (value == null) {
                    !parameterType.isPrimitive
                } else {
                    parameterType.isAssignableFrom(value.portrait)
                }
            }
        } ?: throw IllegalArgumentException(
            "No matching constructor for arguments (${args.joinToString { it?.javaClass?.name ?: "null" }}) on $qualifiedName"
        )

        val parameterTypes = matchingConstructor.parameterTypes.toTypedArray()
        val constructor = getConstructor(*parameterTypes) ?: matchingConstructor
        return constructor.call(*args)
    }

    /**
     * Returns `true` when this descriptor can accept instances described by [other].
     *
     * The default implementation walks the hierarchy via [isSubclassOf]. Providers may override the
     * behavior for performance-sensitive cases (e.g. primitives or generated dispatch tables) but
     * must keep the semantics identical to the default path.
     * Custom overrides must remain reflexive, symmetric with [isSubclassOf], and transitive.
     */
    open fun isAssignableFrom(other: PClass<*>): Boolean {
        return other.isSubclassOf(this)
    }

    /**
     * Returns `true` when this descriptor is equal to or inherits from [other].
     *
     * The default implementation performs a depth-first traversal over [superclass] and
     * [interfaces]. Implementations may override to leverage precomputed hierarchy tables but must
     * keep the result reflexive, transitive, and consistent with [isAssignableFrom].
     */
    open fun isSubclassOf(other: PClass<*>): Boolean {
        if (this === other) return true
        if (this == other) return true
        if (superclass?.isSubclassOf(other) == true) return true
        return interfaces.any { it.isSubclassOf(other) }
    }

    /**
     * Retrieves the first annotation whose [PAnnotation.annotationClass] matches [annotationClass].
     *
     * By default this scans [annotations] linearly. Implementations may override to support faster
     * lookups (for example, by indexing annotations) while preserving nullability semantics.
     */
    open fun getAnnotation(annotationClass: PClass<*>): PAnnotation? {
        return annotations.firstOrNull { it.annotationClass == annotationClass }
    }

    /**
     * Returns `true` when an annotation with the given descriptor is present.
     *
     * The default implementation delegates to [getAnnotation]. Overrides should ensure
     * `hasAnnotation` remains consistent with [getAnnotation] and avoid eagerly materialising
     * additional data.
     */
    open fun hasAnnotation(annotationClass: PClass<*>): Boolean {
        return annotations.any { it.annotationClass == annotationClass }
    }

    /**
     * Finds a constructor whose parameter descriptors match [parameterTypes] exactly.
     *
     * The default implementation performs a linear scan over [constructors]. Providers may override
     * this to leverage precomputed dispatch tables. Overrides must favour exact matches, never
     * return inherited constructors, and keep the behaviour limited to public constructors.
     */
    open fun getConstructor(vararg parameterTypes: PClass<*>): PConstructor<T>? {
        if (parameterTypes.isEmpty()) {
            return constructors.firstOrNull { it.parameterTypes.isEmpty() }
        }

        return constructors.firstOrNull { constructor ->
            if (constructor.parameterTypes.size != parameterTypes.size) return@firstOrNull false
            constructor.parameterTypes.zip(parameterTypes).all { (actual, expected) -> actual == expected }
        }
    }

    /**
     * Finds a declared method by [name] and ordered [parameterTypes].
     *
     * The default implementation performs a linear scan over [methods]. Overrides should keep
     * matching deterministic, may apply provider-specific indexing for faster lookups, and must
     * limit candidates to declared public methods.
     */
    open fun getMethod(name: String, vararg parameterTypes: PClass<*>): PMethod? {
        return methods.firstOrNull { method ->
            if (method.name != name) return@firstOrNull false
            if (method.parameterTypes.size != parameterTypes.size) return@firstOrNull false
            method.parameterTypes.zip(parameterTypes).all { (actual, expected) -> actual == expected }
        }
    }

    /**
     * Returns the declared field whose [PField.name] matches [name], or null when absent.
     *
     * The default implementation performs a linear scan over [fields]. Overrides may precompute a
     * name index but must not surface inherited fields and must only return public fields.
     */
    open fun getField(name: String): PField? {
        return fields.firstOrNull { it.name == name }
    }

    /**
     * Creates a dynamic proxy that implements the type represented by this descriptor.
     *
     * Providers may back this with `java.lang.reflect.Proxy`, generated bytecode, or a native
     * implementation depending on the runtime. The proxy delegates every invocation to [handler],
     * which is responsible for providing results and handling abstract members.
     *
     * Implementations must validate that the represented type can be proxied in the current
     * environment (interfaces and abstract classes only) and should provide meaningful diagnostics
     * when the runtime cannot satisfy the contract.
     *
     * @param handler callback that receives the proxy instance, the invoked method descriptor,
     * and the raw argument array
     * @return a proxy instance compatible with the represented type
     * @throws tech.kaffa.portrait.proxy.ProxyCreationException if the target type cannot be proxied on the current runtime
     * @throws IllegalArgumentException when the descriptor does not represent a proxyable type
     */
    abstract fun createProxy(handler: ProxyHandler<T>): T

    /**
     * Two descriptors are considered equal when they represent the same qualified name.
     *
     * Providers should prefer sharing descriptor instances, but overriding equality ensures that
     * lookups remain consistent even when different providers materialise the same type separately.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PClass<*>) return false
        return qualifiedName == other.qualifiedName
    }

    /**
     * Hash code derived from the qualified name and structural metadata to minimise collisions
     * without triggering recursive member hashing.
     */
    override fun hashCode(): Int {
        fun mix(seed: Int, value: Int): Int = seed * 31 + value

        var result = qualifiedName.hashCode()

        val flags = (if (isAbstract) 1 else 0) or
            (if (isSealed) 1 shl 1 else 0) or
            (if (isData) 1 shl 2 else 0) or
            (if (isCompanion) 1 shl 3 else 0) or
            (if (isPrimitive) 1 shl 4 else 0)
        result = mix(result, flags)

        val superNameHash = superclass?.qualifiedName?.hashCode() ?: 0
        result = mix(result, superNameHash)

        result = mix(result, interfaces.size)
        result = mix(result, constructors.size)
        result = mix(result, methods.size)
        result = mix(result, fields.size)
        result = mix(result, annotations.size)

        return result
    }

    /**
     * Human-readable representation that echoes the type flags and qualified name.
     *
     * Useful for logging and debugging, especially when dealing with generated descriptors.
     */
    override fun toString(): String {
        val typeFlags = buildList {
            if (isAbstract) add("abstract")
            if (isSealed) add("sealed")
            if (isData) add("data")
            if (isCompanion) add("companion")
            if (objectInstance != null) add("object")
        }.joinToString(separator = " ")

        return buildString {
            append("PClass")
            if (typeFlags.isNotEmpty()) append("[$typeFlags]")
            append("(")
            append(qualifiedName)
            append(")")
        }
    }
}
