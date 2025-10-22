import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `java`
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
    testImplementation(testFixtures(project(":tests")))
    testImplementation(project(":portrait-api"))
    testImplementation(libs.junit4)
    testRuntimeOnly(project(":portrait-runtime-jvm"))
}

tasks.withType<Test> {
    useJUnit()
}
