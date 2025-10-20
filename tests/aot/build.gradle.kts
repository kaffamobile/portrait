import java.nio.file.Files
import kotlin.io.path.isRegularFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.teavm)
}

repositories {
    mavenCentral()
    maven("https://repo.teavm.org/maven2")
}

kotlin {
    jvmToolchain(11)
}

val fixturesProject = project(":tests")
val fixturesSourceSets = fixturesProject.extensions.getByType(SourceSetContainer::class.java)
val testFixturesSourceSet = fixturesSourceSets.named("testFixtures").get()

val fixturesClassesDir = fixturesProject.layout.buildDirectory.dir("classes/kotlin/${testFixturesSourceSet.name}")
val compileFixtures = fixturesProject.tasks.named("compileTestFixturesKotlin")
val processFixtureResources = fixturesProject.tasks.named("processTestFixturesResources")

val generatedWrappersDir = layout.projectDirectory.dir(".generated/teavmWrappers/kotlin")
val generatedPortraitDir = layout.buildDirectory.dir("generated/portrait-classes")

sourceSets["test"].kotlin.srcDir(generatedWrappersDir)

dependencies {
    testImplementation(testFixtures(project(":tests")))
    testImplementation(project(":portrait-api"))
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.teavm.junit)
    testRuntimeOnly(project(":portrait-runtime-aot"))
    testRuntimeOnly(files(generatedPortraitDir))
}

val generateTeaVmWrappers = tasks.register("generateTeaVmWrappers") {
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

        val outDir = generatedWrappersDir.asFile
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

tasks.named("compileTestKotlin") {
    dependsOn(generateTeaVmWrappers)
}

val codegenProject = project(":portrait-codegen")
val codegenSourceSets = codegenProject.extensions.getByType(SourceSetContainer::class.java)
val codegenRuntimeClasspath = codegenSourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get().runtimeClasspath

val runPortraitTestsCodegen = tasks.register<JavaExec>("runPortraitTestsCodegen") {
    group = "portrait"
    description = "Runs Portrait codegen on shared test fixtures for TeaVM."

    dependsOn(compileFixtures, codegenProject.tasks.named("assemble"))

    mainClass.set("tech.kaffa.portrait.codegen.cli.PortraitKt")

    doFirst {
        classpath = codegenRuntimeClasspath + files(fixturesClassesDir)
        args = listOf(
            "--input", fixturesClassesDir.get().asFile.absolutePath,
            "--output", generatedPortraitDir.get().asFile.absolutePath
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

val testJs: TaskProvider<*> = tasks.named("test")

testJs.configure {
    //description = "Runs shared tests against the Portrait AOT runtime on TeaVM."
    //group = "verification"

    dependsOn(
        compileFixtures,
        processFixtureResources,
        generateTeaVmWrappers,
        runPortraitTestsCodegen
    )
}
