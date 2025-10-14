package kaffa.portrait.codegen.utils

import io.github.classgraph.ScanResult
import net.bytebuddy.dynamic.ClassFileLocator
import org.slf4j.LoggerFactory

class ClassGraphLocator(
    private val scanResult: ScanResult
) : ClassFileLocator {

    private val logger = LoggerFactory.getLogger(ClassGraphLocator::class.java)

    override fun locate(name: String): ClassFileLocator.Resolution {
        return try {
            val classInfo = scanResult.getClassInfo(name)
                ?: return ClassFileLocator.Resolution.Illegal(name)

            val resource = classInfo.resource
            val bytes = resource.load()
            ClassFileLocator.Resolution.Explicit(bytes)
        } catch (e: IllegalArgumentException) {
            // Handle the case where ScanResult is closed
            logger.debug("ScanResult is closed, cannot locate class file for '$name': ${e.message}")
            ClassFileLocator.Resolution.Illegal(name)
        } catch (e: Exception) {
            logger.debug("Failed to locate class file for '$name': ${e.message}", e)
            ClassFileLocator.Resolution.Illegal(name)
        }
    }

    override fun close() {
    }
}