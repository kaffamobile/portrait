package tech.kaffa.portrait

import tech.kaffa.portrait.internal.BoxedPrimitives

/**
 * Returns the boxed representation of this type when it describes a JVM primitive.
 *
 * - Primitive `PClass` instances resolve to their wrapper descriptors (for example `int` → `java.lang.Integer`).
 * - Wrapper descriptors return themselves.
 * - All other descriptors return `null`.
 *
 * Consumers that need a fallback may prefer [boxed], which returns `this` when no boxed form exists.
 */
fun PClass<*>.boxedOrNull(): PClass<*>? {
    if (isPrimitive) {
        val wrapperName = BoxedPrimitives.boxing[qualifiedName] ?: return null
        return Portrait.forNameOrUnresolved(wrapperName)
    }

    return if (qualifiedName in BoxedPrimitives.unboxing.keys) {
        this
    } else {
        null
    }
}

/**
 * Returns the boxed representation of this type when available, or the receiver when none exists.
 *
 * See [boxedOrNull] for details.
 */
fun PClass<*>.boxed(): PClass<*> = boxedOrNull() ?: this

/**
 * Returns the primitive representation of this type when it describes a boxed primitive.
 *
 * - Wrapper descriptors resolve to their primitive descriptors (for example `java.lang.Integer` → `int`).
 * - Primitive descriptors return themselves.
 * - All other descriptors return `null`.
 *
 * Consumers that need a fallback may prefer [unboxed], which returns `this` when no primitive form exists.
 */
fun PClass<*>.unboxedOrNull(): PClass<*>? {
    if (isPrimitive) {
        return this
    }

    val primitiveName = BoxedPrimitives.unboxing[qualifiedName] ?: return null
    return Portrait.forNameOrUnresolved(primitiveName)
}

/**
 * Returns the primitive representation of this type when available, or the receiver when none exists.
 *
 * See [unboxedOrNull] for details.
 */
fun PClass<*>.unboxed(): PClass<*> = unboxedOrNull() ?: this
