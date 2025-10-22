package tech.kaffa.portrait.codegen.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import org.slf4j.LoggerFactory
import tech.kaffa.portrait.codegen.ClasspathScanner
import tech.kaffa.portrait.codegen.PortraitGenerator
import tech.kaffa.portrait.codegen.PortraitGenerator.OutputType
import tech.kaffa.portrait.codegen.utils.ClasslibConfiguration
import kotlin.io.path.pathString
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

/**
 * Portrait code generator CLI tool.
 *
 * Scans compiled classes for @Reflective and @ProxyTarget annotations
 * and generates Portrait implementation classes for AOT environments.
 *
 * Usage:
 * ```
 * portrait-codegen --input <classpath> --output <jar-path>
 * portrait-codegen --input <classpath> --output <directory> --format folder
 * ```
 */
class Portrait : CliktCommand(
    name = "portrait-codegen",
    help = """
        Portrait AOT Code Generator

        Generates Portrait reflection implementation classes from annotated source classes.
        Scans the provided classpath for @Reflective and @ProxyTarget annotations and
        generates optimized Portrait classes for use in AOT environments (TeaVM, GraalVM, etc).
    """.trimIndent()
) {
    private val logger = LoggerFactory.getLogger(Portrait::class.java)
    private val splash = """
        |  ___         _            _ _
        | | _ \___ _ _| |_ _ _ __ _(_) |_
        | |  _/ _ \ '_|  _| '_/ _` | |  _|
        | |_| \___/_|  \__|_| \__,_|_|\__|
        |                    AoT Generator
    """.trimMargin()


    private val input by option("--input", "-i").required()
        .help("Input classpath (directory or JAR) containing compiled classes to scan")

    private val output by option("--output", "-o")
        .path(canBeFile = true, canBeDir = true).required()
        .help("Output path for generated Portrait classes (JAR file or directory)")

    private val format by option("--format", "-f")
        .choice("jar", "folder", ignoreCase = true)
        .help("Output format: 'jar' or 'folder' (auto-detected from output path if not specified)")

    // New, ergonomic runtime flags
    private val runtimeJars by option("--runtime-jar")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .multiple()
        .help("Add a runtime JAR (repeatable)")

    private val runtimeDirs by option("--runtime-dir")
        .path(mustExist = true, canBeFile = false, canBeDir = true)
        .multiple()
        .help("Add a directory containing runtime .class files (repeatable)")

    private val jreHomes by option("--jre", "--jdk")
        .path(mustExist = true, canBeFile = false, canBeDir = true)
        .multiple()
        .help("Add a JRE/JDK home as the target runtime image (repeatable)")
        .validate { dirs ->
            dirs.forEach { dir ->
                require(looksLikeJreHome(dir)) {
                    "Path '$dir' doesn't look like a JRE/JDK home (expected 'release' file or 'lib/modules'). " +
                            "If you intended a class dir, use --runtime-dir; if a JAR, use --runtime-jar."
                }
            }
        }

    private val teavm by option("--teavm")
        .flag(default = false)
        .help("Include the embedded TeaVM runtime class library")

    private val verbose by option("--verbose", "-v")
        .flag(default = false)
        .help("Print all discovered reflective classes and proxy targets")


    override fun run() {
        printSplash()

        val outputPath = output.pathString
        val outputType = format?.let(::toOutputType) ?: detectOutputType(outputPath)

        val classlib = buildRuntimeConfiguration().createLocator()

        logger.info("Scanning classpath for Portrait annotations...")
        ClasspathScanner(input, classlib).scan().use { scanResult ->
            val reflectiveCount = scanResult.reflectives.size
            val proxyTargetCount = scanResult.proxyTargets.size
            logger.info(
                "Identified ${pluralize(reflectiveCount, "reflective class", "reflective classes")} " +
                        "and ${pluralize(proxyTargetCount, "proxy target")}."
            )

            if (verbose) {
                if (scanResult.reflectives.isEmpty()) {
                    logger.info("Reflective classes: (none)")
                } else {
                    logger.info("Reflective classes:")
                    scanResult.reflectives.sorted().forEach { logger.info(" - $it") }
                }

                if (scanResult.proxyTargets.isEmpty()) {
                    logger.info("Proxy targets: (none)")
                } else {
                    logger.info("Proxy targets:")
                    scanResult.proxyTargets.sorted().forEach { logger.info(" - $it") }
                }
            }

            logger.info("Generating Portrait classes...")
            PortraitGenerator
                .forType(outputType, outputPath, scanResult)
                .use { generator -> generator.generate() }

            logger.info("Portrait code generation completed successfully")
        }
    }

    private fun buildRuntimeConfiguration(): ClasslibConfiguration {
        // Merge new + legacy inputs
        val jarPaths = runtimeJars.map { it.toAbsolutePath().normalize() }.toMutableList()
        val dirPaths = runtimeDirs.map { it.toAbsolutePath().normalize() }.toMutableList()
        val jrePaths = jreHomes.map { it.toAbsolutePath().normalize() }.toMutableList()

        if (teavm) {
            jarPaths.add(loadTeaVmClasslibJar())
            logger.info("Using embedded TeaVM class library")
        }

        if (jarPaths.isEmpty() && dirPaths.isEmpty() && jrePaths.isEmpty()) {
            val javaHome = Paths.get(System.getProperty("java.home")).toAbsolutePath().normalize()
            logger.info("No runtime specified. Falling back to current runtime at $javaHome")
            jrePaths.add(javaHome) // ✅ fix: populate jreHomes, not jarFiles
        }

        return ClasslibConfiguration(
            jarFiles = jarPaths.distinct(),
            directories = dirPaths.distinct(),
            jreHomes = jrePaths.distinct()
        )
    }

    private fun looksLikeJreHome(dir: Path): Boolean {
        return dir.resolve("release").exists() ||
                dir.resolve("lib/modules").exists() ||
                dir.resolve("jre").exists() // lenient for older layouts
    }

    private fun loadTeaVmClasslibJar(): Path {
        val resourcePath = "/META-INF/portrait/teavm-classlib-remapped.jar"
        val input = Portrait::class.java.getResourceAsStream(resourcePath)
            ?: error("Embedded TeaVM class library not found at $resourcePath")

        val tempFile = Files.createTempFile("portrait-teavm-classlib", ".jar")
        tempFile.toFile().deleteOnExit()
        input.use {
            Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }
        return tempFile.toAbsolutePath()
    }

    private fun detectOutputType(path: String): OutputType {
        return if (path.endsWith(".jar", ignoreCase = true))
            OutputType.JAR
        else OutputType.FOLDER
    }

    private fun toOutputType(choice: String): OutputType {
        return when (choice.lowercase()) {
            "jar" -> OutputType.JAR
            "folder" -> OutputType.FOLDER
            else -> error("Unsupported output type: $choice")
        }
    }

    private fun pluralize(count: Int, singular: String, plural: String = "${singular}s"): String {
        return if (count == 1) "1 $singular" else "$count $plural"
    }

    private fun printSplash() {
        echo(splash)
    }
}

fun main(args: Array<String>) = Portrait().main(args)
