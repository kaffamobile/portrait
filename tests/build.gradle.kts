plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    testFixturesImplementation(project(":portrait-api"))
    testFixturesImplementation(libs.kotlin.test)
    testFixturesImplementation(libs.junit4)
}
