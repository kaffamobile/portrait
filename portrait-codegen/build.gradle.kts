import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import tech.kaffa.portrait.codegen.build.TeavmClasslibRemappedJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
    `maven-publish`
//    id("tech.kaffa.portrait.codegen.teavm-classlib")
}

repositories {
    mavenCentral()
}

val teavmClasslib: Configuration by configurations.creating {
    isCanBeResolved = true
}

dependencies {
    implementation(project(":portrait-annotations"))
    implementation(project(":portrait-api"))
    implementation(project(":portrait-runtime-aot"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.bytebuddy)
    implementation(libs.classgraph)
    implementation(libs.kotlinx.metadata.jvm)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.clikt)
    teavmClasslib(libs.teavm.classlib)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.teavm.classlib)
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("tech.kaffa.portrait.codegen.cli.PortraitKt")
}


tasks {
    withType<Test> { useJUnitPlatform() }

    val teavmClasslibRemappedJar by registering(TeavmClasslibRemappedJar::class) {
        sources.setFrom(teavmClasslib)
    }

    processResources {
        dependsOn(teavmClasslibRemappedJar)
        from(teavmClasslibRemappedJar.flatMap { it.archiveFile }) {
            into("META-INF/portrait")
        }
    }

    distTar { enabled = false }
    distZip { enabled = false }
    shadowDistTar { enabled = false }
    shadowDistZip { enabled = false }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
