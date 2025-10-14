package tech.kaffa.portrait.codegen.generator

import org.slf4j.Logger
import tech.kaffa.portrait.codegen.PortraitGenerator
import tech.kaffa.portrait.provider.PortraitProvider
import java.io.File

class DirectoryOutputTarget(
    private val outputDir: File,
    private val logger: Logger
) : OutputTarget {
    init {
        // Ensure output directory exists
        outputDir.mkdirs()
    }

    override fun writeGeneratedClass(generated: PortraitGenerator.GeneratedClass) {
        for ((typeDescription, auxiliaryBytes) in generated.dynamicType.allTypes) {
            val className = typeDescription.name
            val classFilePath = "${className.replace('.', '/')}.class"
            val classFile = File(outputDir, classFilePath)

            classFile.parentFile.mkdirs()
            classFile.writeBytes(auxiliaryBytes)

            logger.debug("Generated: $className")
        }
    }

    override fun writeServiceProviderEntry(providerClassName: String) {
        val serviceFile = File(outputDir, "META-INF/services/${PortraitProvider::class.java.name}")
        serviceFile.parentFile.mkdirs()
        serviceFile.writeText(providerClassName, Charsets.UTF_8)
        logger.debug("Generated service provider entry: ${serviceFile.absolutePath} -> $providerClassName")
    }

    override fun close() {
        // Nothing to close for directory output
    }
}