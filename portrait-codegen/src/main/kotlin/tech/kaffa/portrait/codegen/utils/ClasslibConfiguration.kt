package tech.kaffa.portrait.codegen.utils

import net.bytebuddy.dynamic.ClassFileLocator
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.nio.file.*

/**
 * Configuration describing where class files should be resolved from when running the code generator.
 *
 * @property jarFiles explicit jar files that should be used for class resolution.
 * @property directories directories containing compiled class files.
 * @property jreHomes root directories of JRE/JDK images that should be used for module resolution.
 */
data class ClasslibConfiguration(
    val jarFiles: List<Path> = emptyList(),
    val directories: List<Path> = emptyList(),
    val jreHomes: List<Path> = emptyList()
) {
    init {
        ensureExists("Jar", jarFiles)
        ensureExists("Directory", directories)
        ensureExists("JRE", jreHomes)
    }

    companion object {
        private fun ensureExists(kind: String, paths: List<Path>) {
            paths.forEach {
                require(Files.exists(it)) { "$kind path does not exist: $it" }
            }
        }

        fun forCurrentRuntime(): ClasslibConfiguration =
            ClasslibConfiguration(jreHomes = listOf(Paths.get(System.getProperty("java.home"))))
    }

    fun createLocator(): ClassFileLocator {
        val locators = mutableListOf<ClassFileLocator>()

        jarFiles
            .distinct()
            .mapTo(locators) { ClassFileLocator.ForJarFile.of(it.toFile()) }

        directories
            .distinct()
            .mapTo(locators) { ClassFileLocator.ForFolder(it.toFile()) }

        jreHomes
            .distinct()
            .mapTo(locators) { JrtClassFileLocator(it) }

        return when (locators.size) {
            0 -> ClassFileLocator.NoOp.INSTANCE
            1 -> locators.single()
            else -> ClassFileLocator.Compound(locators)
        }
    }
}

private class JrtClassFileLocator(
    private val javaHome: Path
) : ClassFileLocator {
    private val logger = LoggerFactory.getLogger(JrtClassFileLocator::class.java)
    private val fileSystem: FileSystem
    private val modulesRoot: Path
    private val packagesRoot: Path

    init {
        val env = mapOf("java.home" to javaHome.toAbsolutePath().normalize().toString())
        fileSystem = try {
            FileSystems.newFileSystem(URI.create("jrt:/"), env)
        } catch (alreadyExists: FileSystemAlreadyExistsException) {
            FileSystems.getFileSystem(URI.create("jrt:/"))
        }
        modulesRoot = fileSystem.getPath("modules")
        packagesRoot = fileSystem.getPath("packages")
    }

    override fun locate(name: String): ClassFileLocator.Resolution {
        val internalName = name.replace('.', '/') + CLASS_EXTENSION

        for (module in moduleCandidates(name)) {
            resolveFromModule(module, internalName)?.let { return it }
        }

        // Fall back to checking every module when package hints fail.
        try {
            Files.newDirectoryStream(modulesRoot).use { modules ->
                for (module in modules) {
                    module.fileName?.toString()?.let { moduleName ->
                        resolveFromModule(moduleName, internalName)?.let { return it }
                    }
                }
            }
        } catch (e: IOException) {
            logger.debug("Failed to enumerate modules in $javaHome: ${e.message}", e)
        }

        return ClassFileLocator.Resolution.Illegal(name)
    }

    private fun moduleCandidates(className: String): Sequence<String> {
        val lastDot = className.lastIndexOf('.')
        if (lastDot < 0) return emptySequence()

        val packageName = className.substring(0, lastDot)
        val packagePath = packagesRoot.resolve(packageName)
        if (!Files.exists(packagePath)) return emptySequence()

        return try {
            Files.newDirectoryStream(packagePath).use { modules ->
                modules
                    .mapNotNull { it.fileName?.toString() }
                    .toList()
                    .asSequence()
            }
        } catch (e: IOException) {
            logger.debug("Failed to list package '$packageName' in $javaHome: ${e.message}", e)
            emptySequence()
        }
    }

    private fun resolveFromModule(moduleName: String, internalName: String): ClassFileLocator.Resolution? {
        val classPath = modulesRoot.resolve(moduleName).resolve(internalName)
        return try {
            if (Files.exists(classPath)) {
                ClassFileLocator.Resolution.Explicit(Files.readAllBytes(classPath))
            } else {
                null
            }
        } catch (e: IOException) {
            logger.debug(
                "Failed to load '$internalName' from module '$moduleName' in $javaHome: ${e.message}",
                e
            )
            null
        }
    }

    override fun close() {
        try {
            fileSystem.close()
        } catch (ignored: IOException) {
            logger.debug("Failed to close jrt file system for $javaHome: ${ignored.message}")
        }
    }

    private companion object {
        private const val CLASS_EXTENSION = ".class"
    }
}
