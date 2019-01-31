import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version "1.3.20"
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

group = "org.jordillonch.kes"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.apache.kafka:kafka-clients:2.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.10")
    testImplementation("com.github.javafaker:javafaker:0.16")
    testImplementation("io.mockk:mockk:1.8.10.kotlin13")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}