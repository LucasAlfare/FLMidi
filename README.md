```
███████╗██╗                   ███╗   ███╗██╗██████╗ ██╗
██╔════╝██║                   ████╗ ████║██║██╔══██╗██║
█████╗  ██║         █████╗    ██╔████╔██║██║██║  ██║██║
██╔══╝  ██║         ╚════╝    ██║╚██╔╝██║██║██║  ██║██║
██║     ███████╗              ██║ ╚═╝ ██║██║██████╔╝██║
╚═╝     ╚══════╝              ╚═╝     ╚═╝╚═╝╚═════╝ ╚═╝
```
![](https://jitpack.io/v/LucasAlfare/FLMidi.svg)

FLMidi is a Kotlin library for parsing and writing Standard MIDI Files (SMF) entirely from scratch.

Its main goal is to read and interpret the binary structure of MIDI files, transforming the raw MIDI data into strongly typed Kotlin models while keeping the underlying format accessible for low-level manipulation and study.

The library is built on top of my [custom binary reader and writer library](https://github.com/LucasAlfare/FLBinary).

At the moment, FLBinary loads the entire input into memory instead of reading from a byte stream. Since Standard MIDI Files are generally compact compared to other media formats, this is an acceptable trade-off for the current implementation. Stream-based reading may be added in the future.

> _**🚧 Note:**_ The library is currently focused on Standard MIDI File formats `0` and `1`. Format `2` is not currently a primary target and may not be fully supported yet. See the test fixtures and implementation for the current coverage.

## References and Resources

This project is being developed with the help of the following resources:

- [Standard MIDI File Format — McGill University](http://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html)
- [My custom MIDI format specification (pt-BR)](https://lucasalfare.github.io/blog/2025/07/24/midi_docs/)

# Download

You can include this project in your build using [JitPack](https://jitpack.io/#LucasAlfare/FLMidi).

First, add JitPack to the `repositories` section of your `build.gradle.kts`:

```kotlin
repositories {
  mavenCentral() // for example only
  // ...
  maven("https://jitpack.io")
}
````

Then declare the project dependency in the `dependencies` section:

```kotlin
dependencies {
  // Use the appropriate version tag.
  // Check JitPack for the currently available releases:
  // https://jitpack.io/#LucasAlfare/FLMidi
  implementation("com.github.LucasAlfare:FLMidi:v3")
}
```

# How to Use

FLMidi represents a Standard MIDI File using a small set of Kotlin models.

A MIDI file is represented by a `Midi` object containing:

* `format`
* `division`
* a collection of MIDI tracks

Each track contains a sequence of `Event` objects.

All events share a `deltaTime`, representing the number of MIDI ticks elapsed since the previous event.

## Events

FLMidi currently represents MIDI events through three main categories:

* **Meta Events**
* **Channel Events**
* **System Exclusive (SysEx) Events**

### Meta Events

Meta events describe information contained in the MIDI file itself rather than MIDI channel messages.

Examples include:

* `SequenceNumberMetaEvent`
* `TextMetaEvent`
* `CopyrightNoticeMetaEvent`
* `TrackNameMetaEvent`
* `InstrumentNameMetaEvent`
* `LyricMetaEvent`
* `MarkerMetaEvent`
* `CuePointMetaEvent`
* `MidiChannelPrefixMetaEvent`
* `SetTempoMetaEvent`
* `SmpteOffsetMetaEvent`
* `TimeSignatureMetaEvent`
* `KeySignatureMetaEvent`
* `SequencerSpecificMetaEvent`
* `EndOfTrackMetaEvent`
* `UnknownMetaEvent`

Unknown meta events are preserved through their original type and raw data, allowing MIDI data that is not explicitly modeled by the library to remain accessible.

### Channel Events

Channel events represent MIDI messages associated with a MIDI channel.

Currently supported channel events include:

* `NoteOnControlEvent`
* `NoteOffControlEvent`
* `PolyphonicKeyPressureControlEvent`
* `ControlChangeControlEvent`
* `ProgramChangeControlEvent`
* `ChannelPressureControlEvent`
* `PitchBendControlEvent`

Their properties expose the semantic values of each MIDI message, such as channel, note, velocity, controller, program, pressure, and pitch bend.

### System Exclusive Events

SysEx events are represented by `SysExEvent`.

The model preserves the raw event data and whether the event uses the `0xF7` escaped form.

## Reading MIDI Files

MIDI files can be parsed from their raw bytes using `MidiReader`.

The reader handles the Standard MIDI File binary structure, including:

* `MThd` header chunks
* `MTrk` track chunks
* variable-length quantities
* MIDI channel events
* running status
* meta events
* SysEx events
* format `0`, `1`, and the currently implemented subset of format `2`

Running status is interpreted according to the Standard MIDI File specification, including its cancellation by Meta and SysEx events.

## Writing MIDI Files

MIDI data represented by the Kotlin models can also be serialized back into Standard MIDI File bytes using `MidiWriter`.

The writer supports MIDI running status and can either use or disable it when generating channel events.

For example:

```kotlin
val bytes = MidiWriter.toBytes(
  midi,
  useRunningStatus = true
)
```

The resulting bytes can then be written to a file or processed by another application.

The library also preserves raw data for events such as unknown meta events and SysEx events, allowing them to be read and written without requiring every possible MIDI event type to have a dedicated high-level model.

## Testing

The project includes automated tests covering the MIDI reader and writer.

The test suite includes:

* Standard MIDI format `0` and `1` examples
* MIDI channel events
* Meta events
* Unknown meta events
* Sequencer-specific events
* SysEx events
* Running status
* Variable-length quantities
* Reader/writer round trips
* Binary output validation
* Structural equality of parsed MIDI models

The official MIDI examples used by the tests are available in the test sources.

You can find additional usage examples and implementation details throughout the project's test suite.

# [LICENSE](LICENSE)

MIT License

Copyright (c) 2025 Francisco Lucas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.