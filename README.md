# FLMidi

This is my own library to parse MIDI files using Kotlin language/environment.

This is in development and should help me get binary information that lies in MIDI files and, for example, parse then to other formats.

This project is being built using some oline resources:
- [A tutorial about MIDI specification](https://www.mobilefish.com/tutorials/midi/midi_quickguide_specification.html);
- [Standard MIDI file format](http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html);
- [Implementing a MIDI player in Kotlin from scratch](https://livecoding-recipes.github.io/midi/kotlin/tracker/2022/08/01/implementing-a-midi-tracker-in-kotlin.html) (doesn't contains raw bytes reading implementation, but contains some insights).

# Specification basics

MIDI files stores music in a different way of, e.g., MP3 and WAVE sound formats. Once mp3 and wave sotres in their bytes informations about sound waves (here doesn't matter compression terms) using its physical theory, MIDI files stores sound in a "digital" approach.

For example, in MIDI files we can find information about the music tempo, note that was played or released or even meta informations, such as track names and copyright notices. For this reason, MIDI files are very good to help to deal with contexts that requires manipulation of each individual element from a song/sound.

In terms of bytes structure, the MIDI files basically are composed by "Chunks" of data. Thinking the structure as something like a ".xml" or ".json" hierarchy tree we can visualize a general structure like the following:

```json
"midi": {
  "header": {
    "signature": "MTdH",
    "length": 6,
    "format": 0,
    "numTracks": 1,
    "timeDivision": 96
  }
  
  "track": {
    "signature": "MtrK",
    "numBytes": "x bytes in numeric type",
    "events": [
      "MetaEvent": {
        "deltaTime": 0,
        "trackName": "This is the name of this track!"
      }
    ]
  }
}
```

As you can see, the structure itself is very simple. However, is important to know some details in order to understand those details:

1) In only bytes-reading context, we must to know the right rules to be used in order to read the properly amount of bytes of each information. We must, also, know if, depending the case, we are facing a byte that must be read in a real particular way;
2) In terms of understanding what was actually read we need to understand what the real meaning of each read value and how it can affects the sound itself. Normally this is used when we must to interpret and "play" the MIDI file, instead of only reading its bytes.

## Events

The most important part that can be extracted from MIDI files bytes are the Events. Events, can basically be of three types: `MetaEvent`, `MidiEvent` and `SystemExclusiveEvent`:
- `MetaEvents` express information that is not fundamental to define how the sound actually is, however they express some useful data to properly make the sound/track work in the same behaviour of how it was recorded to the file. For example, the information about `tempo` is stored as an `MetaEvent`;

 [TODO more explanations]

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
