package kaffa.portrait.codegen.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import kaffa.portrait.codegen.ClasspathScanner
import kaffa.portrait.codegen.PortraitGenerator
import org.slf4j.LoggerFactory
import kotlin.io.path.pathString

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
        .path(mustExist = true, canBeFile = true, canBeDir = true)
        .required()
        .help("Input classpath (directory or JAR) containing compiled classes to scan")

    private val output by option("--output", "-o")
        .path(canBeFile = true, canBeDir = true)
        .required()
        .help("Output path for generated Portrait classes (JAR file or directory)")

    private val format by option("--format", "-f")
        .choice("jar", "folder", ignoreCase = true)
        .help("Output format: 'jar' or 'folder' (auto-detected from output path if not specified)")

    override fun run() {
        printSplash()

        // Auto-detect format from output path if not specified
        val outputFormat = format ?: detectOutputFormat(output.pathString)
        val classpath = input.pathString

        logger.info("Scanning classpath for Portrait annotations...")
        val scanResult = ClasspathScanner(classpath).scan()
        logger.info("Identified ${scanResult.reflectives.size} reflective classes and ${scanResult.proxyTargets.size} proxy targets.")

        logger.info("Generating Portrait classes...")
        when (outputFormat) {
            "jar" -> PortraitGenerator.generateJar(scanResult, output.pathString)
            "folder" -> PortraitGenerator.generateFolder(scanResult, output.pathString)
        }

        logger.info("Portrait code generation completed successfully")
    }

    private fun detectOutputFormat(path: String): String {
        return if (path.endsWith(".jar", ignoreCase = true)) {
            "jar"
        } else {
            "folder"
        }
    }

    private fun printSplash() {
        echo(splash)
    }
}

fun main(args: Array<String>) = Portrait().main(args)
