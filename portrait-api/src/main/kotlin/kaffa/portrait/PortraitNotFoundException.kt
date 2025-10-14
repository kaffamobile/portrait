package kaffa.portrait

/**
 * Exception thrown when Portrait cannot resolve a class or create a reflection wrapper.
 *
 * This exception is thrown by Portrait methods when:
 * - No providers are available on the classpath
 * - All available providers return null for a given class name
 * - A class name cannot be resolved to an actual class
 * - A known class/instance cannot be wrapped for reflection
 *
 * The exception message typically indicates the specific context and class name
 * that caused the failure, making it easier to diagnose configuration issues.
 *
 * Common causes:
 * - Missing portrait-runtime-jvm dependency
 * - Typo in class name passed to forName()
 * - Class not available at runtime (missing dependency)
 * - SecurityManager preventing reflection access
 *
 * @param message Descriptive error message indicating what failed
 * @param cause Optional underlying cause of the failure
 */
class PortraitNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)