package tech.kaffa.portrait.proxy

/**
 * Exception thrown when proxy creation fails.
 *
 * This exception indicates that a proxy could not be created for the specified type,
 * typically because the type is not an interface or abstract class, or because the
 * underlying proxy mechanism failed.
 */
class ProxyCreationException @JvmOverloads constructor(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)