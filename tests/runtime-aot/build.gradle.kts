import java.io.File
import java.nio.file.Files
import kotlin.io.path.isRegularFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import java.util.Locale
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.teavm)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

val fixturesProject = project(":tests")
val codegenProject = project(":portrait-codegen")

val generatedWrappersDir = layout.buildDirectory.dir("generated/teavmWrappers/java")
val generatedPortraitDir = layout.buildDirectory.dir("generated/portrait-classes")

sourceSets.test.get().java.srcDir(generatedWrappersDir)

dependencies {
    testImplementation(testFixtures(project(":tests")))
    testImplementation(project(":portrait-api"))
    testImplementation(libs.junit4)
    testImplementation(libs.teavm.junit)
    testImplementation(project(":portrait-runtime-aot"))
    testRuntimeOnly(files(generatedPortraitDir))
}

tasks {
    val testFixturesSourceSet = fixturesProject.extensions
        .getByType(SourceSetContainer::class.java)
        .named("testFixtures")
        .get()

    val fixturesClassesDir = fixturesProject.layout.buildDirectory.dir("classes/java/${testFixturesSourceSet.name}")
    val compileFixtures = fixturesProject.tasks.named("compileTestFixturesJava")
    val processFixtureResources = fixturesProject.tasks.named("processTestFixturesResources")

    val generateTeaVmWrappers by registering {
        group = "portrait"
        description = "Generates TeaVM wrapper subclasses for shared tests."

        dependsOn(compileFixtures)

        inputs.dir(fixturesClassesDir)
        outputs.dir(generatedWrappersDir)

        doLast {
            val compiledDir = fixturesClassesDir.get().asFile
            if (!compiledDir.exists()) {
                logger.lifecycle("No compiled test fixture classes found at ${compiledDir.absolutePath}; skipping wrapper generation.")
                return@doLast
            }

            val outDir = generatedWrappersDir.get().asFile
            if (outDir.exists()) {
                outDir.deleteRecursively()
            }
            outDir.mkdirs()

            Files.walk(compiledDir.toPath()).use { paths ->
                paths.filter { it.isRegularFile() && it.toString().endsWith(".class") }
                    .forEach { classFile ->
                        val relativeName = compiledDir.toPath().relativize(classFile)
                            .toString()
                            .removeSuffix(".class")
                            .replace('\\', '.')
                            .replace('/', '.')

                        if (!relativeName.endsWith("Test") && !relativeName.endsWith("Tests")) return@forEach
                        if ('$' in relativeName) return@forEach

                        val packageName = relativeName.substringBeforeLast('.', missingDelimiterValue = "")
                        val simpleName = relativeName.substringAfterLast('.')
                        val wrapperName = "${simpleName}_TeaVM"

                        val packageBlock = if (packageName.isNotEmpty()) "package $packageName;\n\n" else ""
                        val source = """
                        |${packageBlock}import org.junit.runner.RunWith;
                        |import org.teavm.junit.TeaVMTestRunner;
                        |
                        |@RunWith(TeaVMTestRunner.class)
                        |public class $wrapperName extends $simpleName {
                        |}
                    """.trimMargin()

                        val targetDir = if (packageName.isNotEmpty()) {
                            outDir.resolve(packageName.replace('.', '/')).apply { mkdirs() }
                        } else {
                            outDir
                        }

                        targetDir.resolve("$wrapperName.java").writeText(source)
                    }
            }
        }
    }

    named<JavaCompile>("compileTestJava") {
        dependsOn(generateTeaVmWrappers)
    }

    named<KotlinCompile>("compileTestKotlin") {
        dependsOn(generateTeaVmWrappers)
    }

    val runPortraitTestsCodegen by registering(JavaExec::class) {
        group = "portrait"
        description = "Runs Portrait codegen on shared test fixtures for TeaVM."

        dependsOn(compileFixtures, codegenProject.tasks.named("assemble"))

        mainClass.set("tech.kaffa.portrait.codegen.cli.PortraitKt")

        classpath = codegenProject.extensions
            .getByType<SourceSetContainer>()
            .main
            .get()
            .runtimeClasspath

        doFirst {
            // Exclude the generatedPortraitDir (this task's outputs) from the input classpath
            val outputDir = generatedPortraitDir.get().asFile
            val input = configurations
                .testRuntimeClasspath
                .get()
                .resolve()
                .filterNot { it == outputDir }
                .joinToString(File.pathSeparator) { it.absolutePath }

            args = listOf(
                "--input", input,
                "--output", outputDir.absolutePath,
                "--teavm"
            )
        }
    }

    test {
        dependsOn(
            compileFixtures,
            processFixtureResources,
            generateTeaVmWrappers,
            runPortraitTestsCodegen
        )

        onlyIf {
            val binary = expectedChromeBinary()
            if (binary == null) {
                true
            } else if (isExecutableOnPath(binary)) {
                true
            } else {
                logger.warn(
                    "Skipping $path because '$binary' is not available on PATH. Install Chrome or expose it on PATH to enable TeaVM runtime tests."
                )
                false
            }
        }
    }
}

teavm {
    tests {
        js {
            enabled = true
        }
    }
}

fun expectedChromeBinary(): String? {
    val osName = System.getProperty("os.name").lowercase(Locale.US)
    return when {
        osName.contains("win") -> "chrome.exe"
        osName.contains("nux") || osName.contains("nix") -> "google-chrome-stable"
        else -> null
    }
}

fun isExecutableOnPath(executable: String): Boolean {
    val pathValue = System.getenv("PATH") ?: return false
    return pathValue.split(File.pathSeparator).any { entry ->
        if (entry.isBlank()) {
            false
        } else {
            val candidate = File(entry.trim('"'), executable)
            candidate.exists() && candidate.isFile && candidate.canExecute()
        }
    }
}
