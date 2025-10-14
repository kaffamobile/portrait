package tech.kaffa.portrait.codegen

import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory
import tech.kaffa.portrait.codegen.generator.DirectoryOutputTarget
import tech.kaffa.portrait.codegen.generator.JarOutputTarget
import tech.kaffa.portrait.codegen.generator.OutputTarget
import tech.kaffa.portrait.codegen.portrait.PortraitClassFactory
import tech.kaffa.portrait.codegen.provider.GeneratedPortraitProviderFactory
import tech.kaffa.portrait.codegen.proxy.ProxyClassFactory
import tech.kaffa.portrait.codegen.utils.ExplicitClassLocator
import java.io.Closeable
import java.io.File

class PortraitGenerator private constructor(
    private val output: OutputTarget,
    private val scan: ClasspathScanner.Result
) : Closeable {
    private val byteBuddy = ByteBuddy().with(ClassFileVersion.JAVA_V8)
    private val classpathMap = ExplicitClassLocator()
    private val typePool = TypePool.Default.of(
        ClassFileLocator.Compound(classpathMap, scan.locator)
    )

    interface GeneratedClass {
        val dynamicType: DynamicType
    }

    /** Call when you're done to flush/close underlying resources (e.g., the JarOutputStream). */
    override fun close() {
        output.close()
    }

    /**
     * Generate all proxy/portrait/provider classes and write them to the configured [OutputTarget].
     */
    fun generate() {
        val generatedProxies = mutableMapOf<String, ProxyClassFactory.Result>()

        for (proxy in generateProxyClasses()) {
            generatedProxies[proxy.superType.name] = proxy
            classpathMap.add(proxy.dynamicType)
            output.writeGeneratedClass(proxy)
        }

        val generatedPortraits = generatePortraitClasses(generatedProxies).toList()

        for (portrait in generatedPortraits) {
            classpathMap.add(portrait.dynamicType)
            output.writeGeneratedClass(portrait)
        }

        if (generatedPortraits.isNotEmpty()) {
            generatePortraitProvider(generatedPortraits)
        }
    }

    private fun generateProxyClasses(): Sequence<ProxyClassFactory.Result> {
        val factory = ProxyClassFactory(byteBuddy, typePool)

        return scan.proxyTargets.asSequence()
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
        generatedProxies: MutableMap<String, ProxyClassFactory.Result>
    ): Sequence<PortraitClassFactory.Result> {
        val factory = PortraitClassFactory(byteBuddy, typePool, generatedProxies)

        return (scan.proxyTargets + scan.reflectives).asSequence()
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

    private fun generatePortraitProvider(generatedPortraits: List<PortraitClassFactory.Result>) {
        val providerFactory = GeneratedPortraitProviderFactory(byteBuddy, typePool)
        val providerResult = providerFactory.make(generatedPortraits)

        output.writeGeneratedClass(providerResult)
        output.writeServiceProviderEntry(providerResult.providerClassName)
    }

    private fun shouldGeneratePortrait(className: String): Boolean {
        if (className.isBlank()) return false
        if (className in PRIMITIVE_NAMES) return false
        if (className.endsWith("[]")) return false
        // Allow synthetic nested classes and Kotlin companions (`$`), but ignore simple names without package.
        if (!className.contains('.') && className !in PRIMITIVE_NAMES) return false
        return true
    }

    enum class OutputType { JAR, FOLDER }

    companion object {

        private val logger = LoggerFactory.getLogger(PortraitGenerator::class.java)

        /**
         * Construct a [PortraitGenerator] that writes to a JAR at [outputPath].
         * Remember to call [close] (or use Kotlin's `use {}`) after [generate].
         */
        fun forJar(
            outputPath: String,
            scan: ClasspathScanner.Result
        ): PortraitGenerator {
            val jarFile = File(outputPath)
            return PortraitGenerator(JarOutputTarget(jarFile, logger), scan)
        }

        /**
         * Construct a [PortraitGenerator] that writes class files into the folder at [outputPath].
         * Remember to call [close] (or use Kotlin's `use {}`) after [generate].
         */
        fun forFolder(
            outputPath: String,
            scan: ClasspathScanner.Result
        ): PortraitGenerator {
            val outputDir = File(outputPath)
            return PortraitGenerator(DirectoryOutputTarget(outputDir, logger), scan)
        }

        /**
         * Generic factory that selects the output implementation by [type].
         */
        fun forType(
            type: OutputType,
            outputPath: String,
            scan: ClasspathScanner.Result
        ): PortraitGenerator {
            return when (type) {
                OutputType.JAR -> forJar(outputPath, scan)
                OutputType.FOLDER -> forFolder(outputPath, scan)
            }
        }

        private val PRIMITIVE_NAMES = setOf(
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void"
        )
    }
}
