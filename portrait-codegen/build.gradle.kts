import tech.kaffa.portrait.codegen.build.TeaVmClasslibRemappingTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
    `maven-publish`
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

    val remapTeavmClasslib by registering(TeaVmClasslibRemappingTask::class) {
        sources.setFrom(teavmClasslib)
    }

    processResources {
        dependsOn(remapTeavmClasslib)
        from(remapTeavmClasslib.flatMap { it.outputJar }) {
            into("META-INF/portrait")
        }
    }

    startScripts { enabled = false }
    distTar { enabled = false }
    distZip { enabled = false }
    startShadowScripts { enabled = false }
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
