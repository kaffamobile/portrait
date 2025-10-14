package kaffa.portrait.codegen

import kaffa.portrait.codegen.portrait.PortraitClassFactory
import kaffa.portrait.codegen.provider.GeneratedPortraitProviderFactory
import kaffa.portrait.codegen.proxy.ProxyClassFactory
import kaffa.portrait.codegen.utils.ClassGraphLocator
import kaffa.portrait.codegen.utils.MapClassFileLocator
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.pool.TypePool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedHashSet
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

object PortraitGenerator {
    private val logger = LoggerFactory.getLogger(PortraitGenerator::class.java)

    interface GeneratedClass {
        val dynamicType: DynamicType
    }

    fun generateJar(scan: ClasspathScanner.Result, outputPath: String) {
        val jarFile = File(outputPath)
        // Ensure parent directory exists
        jarFile.parentFile?.mkdirs()

        JarOutputTarget(jarFile, logger).use { output ->
            generate(scan, output)
        }

        logger.info("Generated JAR: ${jarFile.absolutePath}")
    }

    fun generateFolder(scan: ClasspathScanner.Result, outputPath: String) {
        val outputDir = File(outputPath)

        // Ensure output directory exists
        outputDir.mkdirs()

        DirectoryOutputTarget(outputDir, logger).use { output ->
            generate(scan, output)
        }

        logger.info("Generated folder: ${outputDir.absolutePath}")
    }

    private fun generate(
        scan: ClasspathScanner.Result,
        output: GeneratedOutput
    ) {
        val byteBuddy = createByteBuddy()
        val generatedProxies = mutableMapOf<String, ProxyClassFactory.Result>()

        // Create a MapClassFileLocator to hold generated classes
        val classpathMap = MapClassFileLocator()

        // Create composite TypePool that chains the dynamic locator with the classpath scanner's typePool
        val typePool = TypePool.Default.of(
            ClassFileLocator.Compound(
                classpathMap,
                ClassGraphLocator(scan.result),
                ClassFileLocator.ForClassLoader.ofSystemLoader()
            )
        )

        for (proxy in generateProxyClasses(byteBuddy, scan, typePool)) {
            generatedProxies[proxy.superType.name] = proxy
            classpathMap.add(proxy.dynamicType)
            output.writeGeneratedClass(proxy)
        }

        val generatedPortraits = generatePortraitClasses(byteBuddy, scan, generatedProxies, typePool).toList()

        for (portrait in generatedPortraits) {
            classpathMap.add(portrait.dynamicType)
            output.writeGeneratedClass(portrait)
        }

        if (generatedPortraits.isNotEmpty()) {
            val providerFactory = GeneratedPortraitProviderFactory(byteBuddy, typePool)
            val providerResult = providerFactory.make(generatedPortraits)

            output.writeGeneratedClass(providerResult)
            output.writeServiceProviderEntry(providerResult.providerClassName)

            logger.info("Generated PortraitProvider: ${providerResult.providerClassName}")
        }
    }

    private fun createByteBuddy(): ByteBuddy = ByteBuddy().with(ClassFileVersion.JAVA_V8)


    private interface GeneratedOutput : Closeable {
        fun writeGeneratedClass(generated: GeneratedClass)
        fun writeServiceProviderEntry(providerClassName: String)
    }

    private class JarOutputTarget(
        jarFile: File,
        private val logger: Logger
    ) : GeneratedOutput {
        private val jarOut = JarOutputStream(FileOutputStream(jarFile))

        override fun writeGeneratedClass(generated: GeneratedClass) {
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
            val entryName = "META-INF/services/kaffa.portrait.provider.PortraitProvider"
            jarOut.putNextEntry(JarEntry(entryName))
            jarOut.write(providerClassName.toByteArray(Charsets.UTF_8))
            jarOut.closeEntry()
            logger.debug("Generated service provider entry: $entryName -> $providerClassName")
        }

        override fun close() {
            jarOut.close()
        }
    }

    private class DirectoryOutputTarget(
        private val outputDir: File,
        private val logger: Logger
    ) : GeneratedOutput {
        override fun writeGeneratedClass(generated: GeneratedClass) {
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
            val serviceFile = File(outputDir, "META-INF/services/kaffa.portrait.provider.PortraitProvider")
            serviceFile.parentFile.mkdirs()
            serviceFile.writeText(providerClassName, Charsets.UTF_8)
            logger.debug("Generated service provider entry: ${serviceFile.absolutePath} -> $providerClassName")
        }

        override fun close() {
            // Nothing to close for directory output
        }
    }

    private fun generateProxyClasses(
        byteBuddy: ByteBuddy,
        scan: ClasspathScanner.Result,
        typePool: TypePool
    ): Sequence<ProxyClassFactory.Result> {
        val factory = ProxyClassFactory(byteBuddy, typePool)
        val classNames = LinkedHashSet<String>().apply {
            scan.proxyTargets
                .filter { it.isInterface }
                .mapTo(this) { it.name }
            addAll(scan.proxyTargetClassNames)
        }

        return classNames.asSequence()
            .mapNotNull { className ->
                if (!shouldGeneratePortrait(className)) {
                    logger.debug("Skipping proxy generation for $className due to package restrictions")
                    return@mapNotNull null
                }
                try {
                    val typeDescription = typePool.describe(className).resolve()
                    if (!typeDescription.isInterface) {
                        logger.debug("Skipping proxy generation for $className because it is not an interface")
                        null
                    } else {
                        factory.make(typeDescription)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to generate proxy class for $className: ${e.message}", e)
                    null
                }
            }
    }

    private fun generatePortraitClasses(
        byteBuddy: ByteBuddy,
        scan: ClasspathScanner.Result,
        generatedProxies: MutableMap<String, ProxyClassFactory.Result>,
        typePool: TypePool
    ): Sequence<PortraitClassFactory.Result> {
        val factory = PortraitClassFactory(byteBuddy, typePool, generatedProxies)
        val classNames = LinkedHashSet<String>().apply {
            scan.reflectives.mapTo(this) { it.name }
            scan.proxyTargets.mapTo(this) { it.name }
            addAll(scan.reflectiveClassNames)
            addAll(scan.proxyTargetClassNames)
        }

        return classNames.asSequence()
            .filter { shouldGeneratePortrait(it) }
            .mapNotNull { className ->
                try {
                    factory.make(typePool.describe(className).resolve())
                } catch (e: Exception) {
                    logger.warn("Failed to generate portrait class for $className: ${e.message}", e)
                    null
                }
            }
    }

    private fun shouldGeneratePortrait(className: String): Boolean {
        if (className.isBlank()) return false
        if (className in PRIMITIVE_NAMES) return false
        if (className.endsWith("[]")) return false
        // Allow synthetic nested classes and Kotlin companions (`$`), but ignore simple names without package.
        if (!className.contains('.') && className !in PRIMITIVE_NAMES) return false
        return true
    }

    private val PRIMITIVE_NAMES = setOf(
        "boolean",
        "byte",
        "char",
        "short",
        "int",
        "long",
        "float",
        "double",
        "void"
    )
}
