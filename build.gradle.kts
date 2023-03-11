plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "com.lucasalfare.flmidi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.lucasalfare.flbinary:FLBinary:v1.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}