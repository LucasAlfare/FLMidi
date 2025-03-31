plugins {
  alias(libs.plugins.kotlin.jvm)
  application
  `maven-publish`
}

group = "com.lucasalfare.flmidi"
version = "v2.0.0"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
}

dependencies {
  implementation("com.github.LucasAlfare:FLBinary:v1.6")
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(21)
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
