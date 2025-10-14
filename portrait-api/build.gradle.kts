plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":portrait-annotations"))
    api(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testFixturesImplementation(project(":portrait-annotations"))
    testFixturesImplementation(libs.kotlin.stdlib)
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