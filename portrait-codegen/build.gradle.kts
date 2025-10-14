plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    `maven-publish`
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("kaffa.portrait.codegen.cli.PortraitKt")
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
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
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