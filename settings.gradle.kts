import java.net.URI

rootProject.name = "FLMidi"

sourceControl {
  gitRepository(URI("https://github.com/LucasAlfare/FLBinary")) {
    producesModule("com.lucasalfare.flbinary:FLBinary")
  }
}