plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    `maven-publish`
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("tech.kaffa.portrait.codegen.cli.PortraitKt")
}

val teavmClasslib by configurations.creating

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
    implementation(libs.teavm.core)

    teavmClasslib(libs.teavm.classlib)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.teavm.classlib)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.processResources {
    from(teavmClasslib) {
        into("META-INF/portrait/jars")
        rename { "teavm-classlib.jar" }
    }
}

kotlin {
    jvmToolchain(8)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
