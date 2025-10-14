package tech.kaffa.portrait.codegen.generator

import org.slf4j.Logger
import tech.kaffa.portrait.codegen.PortraitGenerator
import tech.kaffa.portrait.provider.PortraitProvider
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class JarOutputTarget(
    jarFile: File,
    private val logger: Logger
) : OutputTarget {
    private val jarOut = JarOutputStream(FileOutputStream(jarFile))

    init {
        // Ensure parent directory exists
        jarFile.parentFile?.mkdirs()
    }

    override fun writeGeneratedClass(generated: PortraitGenerator.GeneratedClass) {
        for ((typeDescription, auxiliaryBytes) in generated.dynamicType.allTypes) {
            val className = typeDescription.name
            val entryName = "${className.replace('.', '/')}.class"

            jarOut.putNextEntry(JarEntry(entryName))
            jarOut.write(auxiliaryBytes)
            jarOut.closeEntry()

            logger.debug("Generated: $className")
        }
    }

    override fun writeServiceProviderEntry(providerClassName: String) {
        val entryName = "META-INF/services/${PortraitProvider::class.java.name}"
        jarOut.putNextEntry(JarEntry(entryName))
        jarOut.write(providerClassName.toByteArray(Charsets.UTF_8))
        jarOut.closeEntry()
        logger.debug("Generated service provider entry: $entryName -> $providerClassName")
    }

    override fun close() {
        jarOut.close()
    }
}