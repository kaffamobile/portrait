package tech.kaffa.portrait.internal

import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.provider.PortraitProvider

/**
 * PortraitProvider for JVM primitive types and well-known array descriptors.
 *
 * This provider handles fundamental Java types that are guaranteed to be available
 * in any Java runtime environment. It provides a high-priority implementation
 * to ensure these core types are handled consistently without using platform reflection.
 *
 * Supported types:
 * - All primitive types: boolean, byte, char, short, int, long, float, double, void
 * - Arrays of the above primitives, boxed primitives, java.lang.String, and java.lang.Object
 */
class InternalPortraitProvider : PortraitProvider {

    override fun priority(): Int = 200 // High priority for well-known types

    override fun <T : Any> forName(className: String): PClass<T>? {
        return when {
            // Handle primitive types
            className in BoxedPrimitives.boxing.keys -> {
                return PrimitivePClass(className, BoxedPrimitives.boxing[className] ?: return null)
            }

            // Handle array types
            className.startsWith("[") -> {
                // Parse array notation like "[I", "[[Ljava.lang.String;", etc.
                if (parseArrayComponentType(className) != null)
                    return ArrayPClass(className)
                else
                    return null
            }

            else -> null
        }
    }

    private fun parseArrayComponentType(arrayClassName: String): String? {
        var className = arrayClassName
        var dimensions = 0

        // Count array dimensions
        while (className.startsWith("[")) {
            dimensions++
            className = className.substring(1)
        }

        if (dimensions == 0) return null

        // Parse component type
        return when {
            className == "Z" -> "boolean"
            className == "B" -> "byte"
            className == "C" -> "char"
            className == "S" -> "short"
            className == "I" -> "int"
            className == "J" -> "long"
            className == "F" -> "float"
            className == "D" -> "double"
            className == "V" -> "void"
            className.startsWith("L") && className.endsWith(";") -> {
                className.substring(1, className.length - 1).replace("/", ".")
            }

            else -> null
        }
    }
}
