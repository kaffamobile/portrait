import java.nio.file.Files
import kotlin.io.path.isRegularFile
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    java
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

val fixturesProject = project(":tests")

val generatedWrappersDir = layout.buildDirectory.dir("generated/testWrappers/java")

sourceSets.test.get().java.srcDir(generatedWrappersDir)

dependencies {
    testImplementation(testFixtures(project(":tests")))
    testImplementation(project(":portrait-api"))
    testImplementation(libs.junit4)
    testImplementation(libs.teavm.junit)
    testImplementation(project(":portrait-runtime-jvm"))
}

tasks {
    val testFixturesSourceSet = fixturesProject.extensions
        .getByType(SourceSetContainer::class.java)
        .named("testFixtures")
        .get()

    val fixturesClassesDir = fixturesProject.layout.buildDirectory.dir("classes/java/${testFixturesSourceSet.name}")
    val compileFixtures = fixturesProject.tasks.named("compileTestFixturesJava")
    val processFixtureResources = fixturesProject.tasks.named("processTestFixturesResources")

    val generateWrappers by registering {
        group = "portrait"
        description = "Generates wrapper subclasses for shared tests."

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
                        val wrapperName = "${simpleName}_JVM"

                        val packageBlock = if (packageName.isNotEmpty()) "package $packageName;\n\n" else ""
                        val source = """
                        |${packageBlock}import org.junit.runner.RunWith;
                        |
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
        dependsOn(generateWrappers)
    }

    named<KotlinCompile>("compileTestKotlin") {
        dependsOn(generateWrappers)
    }

    test {
        useJUnit()

        dependsOn(
            compileFixtures,
            processFixtureResources,
            generateWrappers
        )
    }
}
