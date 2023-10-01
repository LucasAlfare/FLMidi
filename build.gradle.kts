plugins {
  kotlin("jvm") version "1.9.0"
  application
  `maven-publish`
}

group = "com.lucasalfare.flmidi"
version = "v1.0"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
}

dependencies {
  testImplementation(kotlin("test"))
  implementation("com.github.LucasAlfare:FLBinary:1.5")
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

/**
 * Helper block to configure Maven Publishing.
 */
publishing {
  publications {
    create<MavenPublication>("Maven") {
      from(components["kotlin"])
    }
  }
}