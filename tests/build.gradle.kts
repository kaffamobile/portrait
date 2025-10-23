import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java`
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    testFixturesImplementation(project(":portrait-api"))
    testFixturesImplementation(libs.kotlin.stdlib)
    testFixturesImplementation(libs.junit4)
}

kotlin {
    jvmToolchain(11)
}
