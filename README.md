```
███████╗██╗                   ███╗   ███╗██╗██████╗ ██╗
██╔════╝██║                   ████╗ ████║██║██╔══██╗██║
█████╗  ██║         █████╗    ██╔████╔██║██║██║  ██║██║
██╔══╝  ██║         ╚════╝    ██║╚██╔╝██║██║██║  ██║██║
██║     ███████╗              ██║ ╚═╝ ██║██║██████╔╝██║
╚═╝     ╚══════╝              ╚═╝     ╚═╝╚═╝╚═════╝ ╚═╝
```
![](https://jitpack.io/v/LucasAlfare/FLMidi.svg)

This is my own library for parsing MIDI files using the Kotlin programming language — built entirely from scratch.

Its main goal is to extract and interpret binary data contained within MIDI files, enabling deeper understanding and handling meaning of the actual binary data.

For this library, I am using my [custom binary reader helper](https://github.com/LucasAlfare/FLBinary). At this point, the binary reader doesn't support bytes stream yet, so the file bytes are loaded to the memory to be read. Since MIDI doesn't have commonly ultra large files, this should not be a bad point. This should be updated in the future.

> _**🚧 Note:**_ This library is tested against MIDI files of format `0` and `1`, so may be unstable on reading files of format `2`. See the reason at [MidiTests.kt](src/test/kotlin/MidiTests.kt) _kDocs_. 

## References and Resources

This project is being developed with the help of the following resources:

- [Standard MIDI File Format (McGill University)](http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html)
- [Minha especificação customizada do formato MIDI (pt-BR)](https://gist.github.com/LucasAlfare/c4197b1b4776d4061b36cf6e99d06754)

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

You can take more deep examples in the main project [test file](src/test/kotlin/MidiTests.kt), from tests sources package.

# [LICENSE](LICENSE)
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