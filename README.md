```
███████╗██╗                   ███╗   ███╗██╗██████╗ ██╗
██╔════╝██║                   ████╗ ████║██║██╔══██╗██║
█████╗  ██║         █████╗    ██╔████╔██║██║██║  ██║██║
██╔══╝  ██║         ╚════╝    ██║╚██╔╝██║██║██║  ██║██║
██║     ███████╗              ██║ ╚═╝ ██║██║██████╔╝██║
╚═╝     ╚══════╝              ╚═╝     ╚═╝╚═╝╚═════╝ ╚═╝
```

This is my own library for parsing MIDI files using the Kotlin programming language — built entirely from scratch.

> 🚧 This library may be unstable for some Control Events.

Its main goal is to extract and interpret binary data contained within MIDI files, enabling deeper understanding and manipulation of the format.

## References and Resources

This project is being developed with the help of the following resources:

- [Standard MIDI File Format (McGill University)](http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html)
- [Minha especificação customizada do formato MIDI (pt-BR)](https://gist.github.com/LucasAlfare/c4197b1b4776d4061b36cf6e99d06754)[]

# Download
You can include this project in your build using [JitPack](https://jitpack.io/#LucasAlfare/FLMidi).

To do this, first add JitPack to the `repositories` section of your `build.gradle.kts` file:
```kotlin
repositories {
  mavenCentral() // for example only
  // ...
  maven("https://jitpack.io") // this enables you to search in Jitpack
}
```

Next, declare the project dependency in the `dependencies` section of the same `build.gradle.kts` file as follows:
```kotlin
dependencies {
  // make sure to the right version tag
  // for reference, check the Jitpack link for current available releases:
  // https://jitpack.io/#LucasAlfare/FLMidi
  implementation("com.github.LucasAlfare:FLMidi:v2.1.1")
}
```

# How to Use

This library is built around the concept that MIDI files are composed of `Events`. Within a MIDI file, there are three main categories of events:

- `MetaEvents`
- `ControlEvents`
- `SystemExclusiveEvents`

Each of these event types contains the following common properties:

- `deltaTime`: the time difference (in ticks) since the previous event;
- `data`: the raw data associated with the event.

Each event type may represent different kinds of events, and the structure or meaning of their data varies. You can find detailed explanations about these data values in the official MIDI file format specification.

Based on this structure, the library parses all MIDI data into Kotlin models, making it accessible and easy to use in code.

For example, to get the number of `Tracks` in a MIDI file, you can do:
```kotlin
fun main() {
  val myMidi = readMidi(
    "path/to/my/great/midi/file.mid"
  )
  println(myMidi.header.numTracks)
}
```

Note that the root parsing function returns a `Midi` object, which provides access to several useful fields.

For example, to check how many meta events are present in a specific track, you can do:
```kotlin
fun main() {
  val myMidi = readMidi(
    "path/to/my/great/midi/file.mid"
  )
  println(
    myMidi
      .header
      .tracks
      .first()
      .events
      .filter {
        it is MetaEvent
      }
      .size
  )
}
```
# License

```
MIT License

Copyright (c) 2025 Francisco Lucas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
