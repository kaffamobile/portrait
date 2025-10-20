import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
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
    testFixturesImplementation(libs.junit4)
}
