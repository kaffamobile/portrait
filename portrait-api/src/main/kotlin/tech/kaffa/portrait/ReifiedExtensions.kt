package tech.kaffa.portrait

/**
 * Reified convenience wrappers for APIs that normally require a [PClass] argument.
 *
 * These helpers allow callers to pass type parameters directly instead of manually resolving
 * descriptor instances.
 */

/** Returns the annotation of type [A] present on this class, or null when absent. */
inline fun <reified A : Annotation> PClass<*>.getAnnotation(): PAnnotation<A>? {
    val annotationDescriptor = A::class.portrait
    return getAnnotation(annotationDescriptor)
}

/** Returns `true` when this class declares an annotation of type [A]. */
inline fun <reified A : Annotation> PClass<*>.hasAnnotation(): Boolean =
    hasAnnotation(A::class.portrait)

/** Returns the annotation of type [A] present on this constructor, or null when absent. */
inline fun <reified A : Annotation> PConstructor<*>.getAnnotation(): PAnnotation<A>? {
    val annotationDescriptor = A::class.portrait
    return getAnnotation(annotationDescriptor)
}

/** Returns `true` when this constructor declares an annotation of type [A]. */
inline fun <reified A : Annotation> PConstructor<*>.hasAnnotation(): Boolean =
    hasAnnotation(A::class.portrait)

/** Returns the annotation of type [A] present on this method, or null when absent. */
inline fun <reified A : Annotation> PMethod.getAnnotation(): PAnnotation<A>? {
    val annotationDescriptor = A::class.portrait
    return getAnnotation(annotationDescriptor)
}

/** Returns `true` when this method declares an annotation of type [A]. */
inline fun <reified A : Annotation> PMethod.hasAnnotation(): Boolean =
    hasAnnotation(A::class.portrait)

/** Returns the annotation of type [A] present on this field, or null when absent. */
inline fun <reified A : Annotation> PField.getAnnotation(): PAnnotation<A>? {
    val annotationDescriptor = A::class.portrait
    return getAnnotation(annotationDescriptor)
}

/** Returns `true` when this field declares an annotation of type [A]. */
inline fun <reified A : Annotation> PField.hasAnnotation(): Boolean =
    hasAnnotation(A::class.portrait)

/** Returns `true` when this descriptor can accept instances of [T]. */
inline fun <reified T : Any> PClass<*>.isAssignableFrom(): Boolean =
    isAssignableFrom(T::class.portrait)

/** Returns `true` when this descriptor represents a subclass of [T]. */
inline fun <reified T : Any> PClass<*>.isSubclassOf(): Boolean =
    isSubclassOf(T::class.portrait)
