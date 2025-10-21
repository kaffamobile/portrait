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
import tech.kaffa.portrait.codegen.utils.ClasslibLocatorFactory
import java.io.File
import kotlin.io.path.pathString
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

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

    private val input by option("--input", "-i")
        .required()
        .help("Input classpath (directory or JAR) containing compiled classes to scan")

    private val output by option("--output", "-o")
        .path(canBeFile = true, canBeDir = true)
        .required()
        .help("Output path for generated Portrait classes (JAR file or directory)")

    // Accepts "jar" or "folder" (case-insensitive) and defers to OutputType
    private val format by option("--format", "-f")
        .choice("jar", "folder", ignoreCase = true)
        .help("Output format: 'jar' or 'folder' (auto-detected from output path if not specified)")

    private val classlibJars by option("--classlib-jar")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .multiple()
        .help("Jar file providing the target class library for resolution (repeatable)")

    private val classlibDirs by option("--classlib-dir")
        .path(mustExist = true, canBeFile = false, canBeDir = true)
        .multiple()
        .help("Directory containing class files that make up the target class library (repeatable)")

    private val classlibJres by option("--classlib-jre")
        .path(mustExist = true, canBeFile = false, canBeDir = true)
        .multiple()
        .help("Root directory of a JRE/JDK image to use as the target class library (repeatable)")

    private val teaVm by option("--teavm")
        .flag(default = false)
        .help("Use the embedded TeaVM runtime class library for resolution")

    override fun run() {
        printSplash()

        val classpath = input
        val outputPath = output.pathString

        // Auto-detect OutputType from the output path if not specified
        val outputType: OutputType = format
            ?.let { toOutputType(it) }
            ?: detectOutputType(outputPath)

        val classlibConfig = buildClasslibConfiguration()
        val classlibLocator = ClasslibLocatorFactory.create(classlibConfig)

        logger.info("Scanning classpath for Portrait annotations...")
        ClasspathScanner(classpath, classlibLocator).scan().use { scanResult ->
            logger.info(
                "Identified ${scanResult.reflectives.size} reflective classes and " +
                        "${scanResult.proxyTargets.size} proxy targets."
            )

            logger.info("Generating Portrait classes...")
            PortraitGenerator
                .forType(outputType, outputPath, scanResult)
                .use { generator -> generator.generate() }

            logger.info("Portrait code generation completed successfully")
        }
    }

    private fun buildClasslibConfiguration(): ClasslibConfiguration {
        val jarPaths = classlibJars.map { it.toAbsolutePath().normalize() }.toMutableList()
        val dirPaths = classlibDirs.map { it.toAbsolutePath().normalize() }.toMutableList()
        val jrePaths = classlibJres.map { it.toAbsolutePath().normalize() }.toMutableList()

        if (teaVm) {
            jarPaths.add(loadTeaVmClasslibJar())
            logger.info("Using embedded TeaVM class library")
        }

        if (jarPaths.isEmpty() && dirPaths.isEmpty() && jrePaths.isEmpty()) {
            val javaHome = Paths.get(System.getProperty("java.home")).toAbsolutePath().normalize()
            logger.info("No class library specified. Falling back to current runtime at $javaHome")
            jarPaths.add(javaHome)
        }

        return ClasslibConfiguration(
            jarFiles = jarPaths.distinct(),
            directories = dirPaths.distinct(),
            jreHomes = jrePaths.distinct()
        )
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

    private fun printSplash() {
        echo(splash)
    }
}

fun main(args: Array<String>) = Portrait().main(args)
