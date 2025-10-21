import java.nio.file.Files
import kotlin.io.path.isRegularFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

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

val generatedWrappersDir = layout.buildDirectory.dir("generated/teavmWrappers/kotlin")
val generatedPortraitDir = layout.buildDirectory.dir("generated/portrait-classes")

sourceSets.test.get().kotlin.srcDir(generatedWrappersDir)

dependencies {
    testImplementation(testFixtures(project(":tests")))
    testImplementation(project(":portrait-api"))
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.teavm.junit)
    testRuntimeOnly(project(":portrait-runtime-aot"))
    testRuntimeOnly(files(generatedPortraitDir))
}

tasks {
    val testFixturesSourceSet = fixturesProject.extensions
        .getByType(SourceSetContainer::class.java)
        .named("testFixtures")
        .get()

    val fixturesClassesDir = fixturesProject.layout.buildDirectory.dir("classes/kotlin/${testFixturesSourceSet.name}")
    val compileFixtures = fixturesProject.tasks.named("compileTestFixturesKotlin")
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

                        val packageBlock = if (packageName.isNotEmpty()) "package $packageName\n\n" else ""
                        val source = """
                        |${packageBlock}import org.junit.runner.RunWith
                        |import org.teavm.junit.TeaVMTestRunner
                        |
                        |@RunWith(TeaVMTestRunner::class)
                        |class $wrapperName : $simpleName()
                    """.trimMargin()

                        val targetDir = if (packageName.isNotEmpty()) {
                            outDir.resolve(packageName.replace('.', '/')).apply { mkdirs() }
                        } else {
                            outDir
                        }

                        targetDir.resolve("$wrapperName.kt").writeText(source)
                    }
            }
        }
    }

    compileTestKotlin {
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
            val input = configurations
                .testRuntimeClasspath
                .get()
                .resolve()
                .joinToString(File.pathSeparator) { it.absolutePath }

            args = listOf(
                "--input", input,
                "--output", generatedPortraitDir.get().asFile.absolutePath,
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
    }
}

teavm {
    tests {
        js {
            enabled = true
        }
    }
}
