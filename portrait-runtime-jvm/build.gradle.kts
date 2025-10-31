plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":portrait-api"))
    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
