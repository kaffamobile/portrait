plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    // Uncomment for TeaVM support (requires Java 11+):
    // alias(libs.plugins.teavm)
}

repositories {
    mavenCentral()
}

dependencies {
    // Test fixtures (src/main) - annotated classes for codegen
    implementation(project(":portrait-annotations"))
    implementation(libs.kotlin.stdlib)

    // E2E tests (src/test) - test both JVM and TeaVM
    testImplementation(project(":portrait-api"))
    testImplementation(project(":portrait-runtime-aot"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)

    // Add generated portrait classes to test runtime classpath
    testRuntimeOnly(files(layout.buildDirectory.dir("generated/portrait-classes")))

    // Note: TeaVM requires Java 11+. To enable TeaVM tests:
    // 1. Switch to Java 11+ toolchain
    // 2. Uncomment: testImplementation(libs.teavm.junit)
    // 3. Uncomment the teavm plugin above
}

kotlin {
    jvmToolchain(8)
}

// Configure JUnit for JVM tests
tasks.withType<Test> {
    useJUnitPlatform()
}

// Task to run codegen on test fixtures (src/main) - Folder mode
val runCodegen by tasks.registering(JavaExec::class) {
    dependsOn(tasks.compileKotlin, project(":portrait-codegen").tasks.assemble)

    group = "portrait"
    description = "Run Portrait codegen on test fixtures (Folder mode)"

    // Include both codegen runtime classpath AND the compiled classes being processed
    classpath = project(":portrait-codegen").sourceSets.main.get().runtimeClasspath +
                files(layout.buildDirectory.dir("classes/kotlin/main"))
    mainClass.set("kaffa.portrait.codegen.cli.PortraitKt")

    // Pass the compiled classes directory and output folder path
    args = listOf(
        "--input", layout.buildDirectory.dir("classes/kotlin/main").get().asFile.absolutePath,
        "--output", layout.buildDirectory.dir("generated/portrait-classes").get().asFile.absolutePath
    )
}

// Add generated sources to main source set
kotlin.sourceSets.main {
    kotlin.srcDir(layout.buildDirectory.dir("generated/sources/portrait/kotlin/main"))
}

// Ensure codegen runs before compiling tests
tasks.compileTestKotlin {
    dependsOn(runCodegen)
}

// Note: TeaVM Gradle plugin configuration requires Java 11+
// To run tests on TeaVM, you'll need to:
// 1. Use Java 11+ for the Gradle daemon
// 2. Uncomment the teavm plugin above
// 3. Add the configuration below:
//
// teavm {
//     tests {
//         js {
//             enabled = true
//         }
//     }
// }
//
// Then run: ./gradlew :tests-e2e:testJs
