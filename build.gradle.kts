plugins {
  kotlin("jvm") version "1.9.0"
  application
  `maven-publish`
}

group = "com.lucasalfare.flmidi"
version = "v1.0.2"

/*
repositories {
  mavenCentral()
  maven("https://jitpack.io")
}
*/

dependencies {
  testImplementation(kotlin("test"))
  implementation("com.github.LucasAlfare:FLBinary:v1.5")
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(8)
}

application {
  mainClass.set("com.lucasalfare.flmidi.MainKt")
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
