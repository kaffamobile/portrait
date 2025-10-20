plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    testImplementation(testFixtures(project(":tests")))
    testImplementation(project(":portrait-api"))
    testRuntimeOnly(project(":portrait-runtime-jvm"))
    testImplementation(libs.kotlin.test.junit)
}

tasks.withType<Test> {
    useJUnit()
}
