# FLMidi

This is my own library to parse MIDI files using Kotlin language/environment.

This is in development and should help me get binary information that lies in MIDI files and, for example, parse then to other formats.

This project is being built using some oline resources:
- [A tutorial about MIDI specification](https://www.mobilefish.com/tutorials/midi/midi_quickguide_specification.html);
- [Standard MIDI file format](http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html);
- [Implementing a MIDI player in Kotlin from scratch](https://livecoding-recipes.github.io/midi/kotlin/tracker/2022/08/01/implementing-a-midi-tracker-in-kotlin.html) (doesn't contains raw bytes reading implementation, but contains some insights).

# Download

Currently is in a rough development phase but should be fine to use in some pushes. But if you say... _I don care. Let me use as it is_ you can grab this project directly from its [GitHub page](https://github.com/LucasAlfare/FLMidi) with [Source Dependencies](https://blog.gradle.org/introducing-source-dependencies), from Gradle tool. First, add this to your `settings.gradle.kts`:

```kotlin
sourceControl {
  gitRepository(URI("https://github.com/LucasAlfare/FLMidi")) {
    producesModule("com.lucasalfare.flbinary:FLMidi")
  }
}
```

After, add this to your `build.gradle.kts` to target the `master` branch of this repository:

```kotlin
implementation("com.lucasalfare.flbinary:FLMidi") {
  version {
    branch = "master"
  }
}
```

# How to use

This library has been built using the concept that MIDI files are composed by `Events`. In a MIDI file we can find three categories of events: `MetaEvents` `ControlEvents` and `SystemExclusiveEvents`. All these events categories will always contain the following information:

- `deltaTime`: indicates the current time diff that this event occuried;
- `data`: the actual data associated tho this event.

All events have their own possible events and each event has its own data. The meaning behind each data value can be found in the MIDI format file specification.

Knowing this, this library is able to parse all those information to Kotlin code, and it exposes it to be used. For example, to check how many `Tracks` a MIDI file contains, we can run:
```kotlin
import com.lucasalfare.flmidi.loadAndReadMidiFile

fun main() {
  val myMidiInfo = loadAndReadMidiFile(
    "path/to/my/great/midi/file.mid"
  )
  println(midiInfo.header.numTracks)
}
```

Note that this root reading function returns a `MidiInfo` object, that contains other useful fields. For example, to check how many meta events are contained in a track, you can do:

```kotlin
import com.lucasalfare.flmidi.loadAndReadMidiFile

fun main() {
  val myMidiInfo = loadAndReadMidiFile(
    "path/to/my/great/midi/file.mid"
  )
  println(
    midiInfo
      .header
      .tracks
      .first()
      .events
      .filter {
        it.category == EventCategory.Meta
      }
      .size
  )
}
```