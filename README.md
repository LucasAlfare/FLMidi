```

███████╗██╗                   ███╗   ███╗██╗██████╗ ██╗
██╔════╝██║                   ████╗ ████║██║██╔══██╗██║
█████╗  ██║         █████╗    ██╔████╔██║██║██║  ██║██║
██╔══╝  ██║         ╚════╝    ██║╚██╔╝██║██║██║  ██║██║
██║     ███████╗              ██║ ╚═╝ ██║██║██████╔╝██║
╚═╝     ╚══════╝              ╚═╝     ╚═╝╚═╝╚═════╝ ╚═╝
                                                       

```

This is my own library to parse MIDI files using Kotlin language/environment. From absolutely scratch.

This is in development and should help me get binary information that lies in MIDI files.

This project is being built using some online resources:
- [Standard MIDI file format](http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html);
- [My custom MIDI format specification in pt-BR](https://gist.github.com/LucasAlfare/c4197b1b4776d4061b36cf6e99d06754).

# Download
You can grab this project using [JitPack](https://jitpack.io/#LucasAlfare/FLMidi). For this, add Jitpack as a dependency source in the repositories section of your `build.gradle.kts`:
```kotlin
repositories {
  mavenCentral() // for example only
  // ...
  maven("https://jitpack.io") // this enables you to search in Jitpack
}
```

After, you can just declare this project in `dependencies` section of the same `build.gradle.kts` file as following:
```kotlin
dependencies {
  // make sure to the right version tag
  // for reference, check the Jitpack link for current available releases:
  // https://jitpack.io/#LucasAlfare/FLMidi
  implementation("com.github.LucasAlfare:FLMidi:v2.0.0")
}
```

# How to use

This library has been built using the concept that MIDI files are composed by `Events`. In a MIDI file we can find three categories of events: `MetaEvents` `ControlEvents` and `SystemExclusiveEvents`. All these events categories will always contain the following information:

- `deltaTime`: indicates the current time diff that this event occurred;
- `data`: the actual data associated tho this event.

All events have their own possible events and each event has its own data. The meaning behind each data value can be found in the MIDI format file specification.

Knowing this, this library is able to parse all those information to Kotlin code, and it exposes it to be used. For example, to check how many `Tracks` a MIDI file contains, we can run:
```kotlin
fun main() {
  val myMidi = readMidi(
    "path/to/my/great/midi/file.mid"
  )
  println(myMidi.header.numTracks)
}
```

Note that this root reading function returns a `Midi` object, that contains other useful fields. For example, to check how many meta events are contained in a track, you can do:

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
