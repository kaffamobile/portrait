package kaffa.portrait.codegen.utils

import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import java.util.concurrent.ConcurrentHashMap

/**
 * A ClassFileLocator that resolves class files from a map of in-memory byte arrays.
 *
 * This locator is used during code generation to resolve classes that have been
 * dynamically generated but not yet written to disk or loaded into a ClassLoader.
 */
class MapClassFileLocator : ClassFileLocator {
    private val classFiles = ConcurrentHashMap<String, ByteArray>()

    /**
     * Add a DynamicType and all its auxiliary types to this locator.
     */
    fun add(dynamicType: DynamicType) {
        for ((typeDescription, bytes) in dynamicType.allTypes) {
            classFiles[typeDescription.name] = bytes
        }
    }

    override fun locate(name: String): ClassFileLocator.Resolution {
        val bytes = classFiles[name]
        return if (bytes != null) {
            ClassFileLocator.Resolution.Explicit(bytes)
        } else {
            ClassFileLocator.Resolution.Illegal(name)
        }
    }

    override fun close() {
    }
}
