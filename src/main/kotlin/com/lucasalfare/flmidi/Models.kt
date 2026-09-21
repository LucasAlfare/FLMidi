// MidiModel.kt
@file:Suppress("ArrayInDataClass")

package com.lucasalfare.flmidi

/**
 * Represents a Standard MIDI File (SMF).
 *
 * An SMF is composed of a file header and one or more tracks. This class
 * represents the decoded, high-level form of that data after the binary
 * structure of the MIDI file has been parsed.
 *
 * The [tracks] property contains the events belonging to each MIDI track.
 * Events are kept in their original track order, and each event contains
 * its delta-time relative to the previous event in that track.
 *
 * ### MIDI file structure
 *
 * Conceptually, an SMF can be represented as:
 *
 * ```
 * MIDI file
 * ├── Header
 * │   ├── Format
 * │   ├── Number of tracks
 * │   └── Division
 * │
 * └── Tracks
 *     ├── Track 1
 *     │   ├── Event
 *     │   ├── Event
 *     │   └── ...
 *     ├── Track 2
 *     │   ├── Event
 *     │   └── ...
 *     └── ...
 * ```
 *
 * This class intentionally stores tracks as `List<List<Event>>` rather than
 * exposing the binary representation of the MIDI file. This makes the model
 * suitable for higher-level operations such as inspecting events, editing
 * tracks, calculating timing, or eventually writing a new MIDI file.
 *
 * @property format the SMF format type. MIDI defines formats `0`, `1`, and `2`.
 * @property division the MIDI time-division value from the file header. When
 * the most significant bit is clear, it represents ticks per quarter note.
 * When set, it represents an SMPTE-based timing format.
 * @property tracks the tracks contained in the MIDI file. Each inner list
 * contains the events belonging to one track, in file order.
 */
data class Midi(
  val format: Int,
  val division: Int,
  val tracks: List<List<Event>>
)

/**
 * Base type for all MIDI events.
 *
 * Every event stored inside an SMF track has a delta-time associated with it.
 * The delta-time specifies how many MIDI time units must elapse after the
 * previous event before this event occurs.
 *
 * Delta-time is therefore relative rather than absolute.
 *
 * For example:
 *
 * ```
 * Event A: deltaTime = 0
 * Event B: deltaTime = 120
 * Event C: deltaTime = 240
 * ```
 *
 * means that:
 *
 * - Event A occurs immediately.
 * - Event B occurs 120 ticks after Event A.
 * - Event C occurs 240 ticks after Event B.
 *
 * The absolute position of an event can be calculated by accumulating the
 * delta-times from the beginning of the track.
 *
 * This class is open so that concrete MIDI event types can inherit from it.
 *
 * @property deltaTime the number of MIDI time units elapsed since the
 * previous event in the same track.
 */
open class Event(open val deltaTime: Int)

// ---------------------------------------------------------
// META EVENTS
// ---------------------------------------------------------

/**
 * Represents the Sequence Number meta event (`FF 00`).
 *
 * This event optionally identifies a MIDI sequence with a sequence number.
 * It is primarily metadata and does not directly produce sound.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property sequenceNumber the sequence number associated with the track.
 */
data class SequenceNumberMetaEvent(
  override val deltaTime: Int,
  val sequenceNumber: Int
) : Event(deltaTime)

/**
 * Represents a generic Text meta event (`FF 01`).
 *
 * This event stores arbitrary textual information associated with the MIDI
 * sequence.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property text the textual content of the event.
 */
data class TextMetaEvent(
  override val deltaTime: Int,
  val text: String
) : Event(deltaTime)

/**
 * Represents the Copyright Notice meta event (`FF 02`).
 *
 * This event contains copyright information associated with the MIDI file
 * or its musical content.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property copyrightNotice the copyright notice text.
 */
data class CopyrightNoticeMetaEvent(
  override val deltaTime: Int,
  val copyrightNotice: String
) : Event(deltaTime)

/**
 * Represents the Track Name meta event (`FF 03`).
 *
 * This event provides a human-readable name for the track.
 *
 * For example, a MIDI file containing an orchestral arrangement might use
 * names such as `"Clarinet 1"`, `"Piano"`, or `"Violins"`.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property trackName the name assigned to the track.
 */
data class TrackNameMetaEvent(
  override val deltaTime: Int,
  val trackName: String
) : Event(deltaTime)

/**
 * Represents the Instrument Name meta event (`FF 04`).
 *
 * This event provides a human-readable name for the instrument represented
 * by the track.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property instrumentName the instrument name.
 */
data class InstrumentNameMetaEvent(
  override val deltaTime: Int,
  val instrumentName: String
) : Event(deltaTime)

/**
 * Represents the Program Name meta event (`FF 08`).
 *
 * This event contains a human-readable name associated with the program or
 * patch used by the track.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property programName the program name.
 */
data class ProgramNameMetaEvent(
  override val deltaTime: Int,
  val programName: String
) : Event(deltaTime)

/**
 * Represents the Device (Port) Name meta event (`FF 09`).
 *
 * This event identifies the MIDI device or port associated with the track.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property deviceName the device or port name.
 */
data class DeviceNameMetaEvent(
  override val deltaTime: Int,
  val deviceName: String
) : Event(deltaTime)

/**
 * Represents a Lyric meta event (`FF 05`).
 *
 * This event stores a lyric or fragment of lyric text associated with the
 * current musical position.
 *
 * Multiple lyric events can be used throughout a track to represent the
 * progression of lyrics over time.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property lyric the lyric text.
 */
data class LyricMetaEvent(
  override val deltaTime: Int,
  val lyric: String
) : Event(deltaTime)

/**
 * Represents a Marker meta event (`FF 06`).
 *
 * Markers provide named points within a MIDI sequence. They can be used to
 * identify musical or structural locations such as sections of a song.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property marker the marker text.
 */
data class MarkerMetaEvent(
  override val deltaTime: Int,
  val marker: String
) : Event(deltaTime)

/**
 * Represents a Cue Point meta event (`FF 07`).
 *
 * Cue points identify a particular location or event in the sequence.
 * Unlike a marker, a cue point is commonly associated with an external
 * action or synchronization point.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property cuePoint the cue point description.
 */
data class CuePointMetaEvent(
  override val deltaTime: Int,
  val cuePoint: String
) : Event(deltaTime)

/**
 * Represents the MIDI Channel Prefix meta event (`FF 20`).
 *
 * This event associates subsequent MIDI events with a specific MIDI channel.
 * The channel value is represented using the lower four bits of the event's
 * data byte.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property midiChannelPrefix the MIDI channel associated with the prefix.
 */
data class MidiChannelPrefixMetaEvent(
  override val deltaTime: Int,
  val midiChannelPrefix: Int
) : Event(deltaTime)

/**
 * Represents the End of Track meta event (`FF 2F`).
 *
 * This event marks the logical end of a MIDI track. It does not produce sound;
 * instead, it tells an SMF reader that no more events belong to the track.
 *
 * The event itself has no additional payload.
 *
 * @property deltaTime the time elapsed since the previous event.
 */
data class EndOfTrackMetaEvent(
  override val deltaTime: Int
) : Event(deltaTime)

/**
 * Represents the Set Tempo meta event (`FF 51`).
 *
 * The tempo value is stored as the number of microseconds per quarter note
 * (MPQN).
 *
 * For example:
 *
 * ```
 * tempo = 500000
 * ```
 *
 * corresponds to 120 BPM because one quarter note takes 500,000 microseconds
 * (0.5 seconds).
 *
 * BPM can therefore be calculated as:
 *
 * ```
 * BPM = 60,000,000 / tempo
 * ```
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property tempo the duration of one quarter note in microseconds.
 */
data class SetTempoMetaEvent(
  override val deltaTime: Int,
  val tempo: Int
) : Event(deltaTime)

/**
 * Represents the SMPTE Offset meta event (`FF 54`).
 *
 * This event specifies a time offset using SMPTE time-code components.
 * SMPTE timing is commonly used when synchronizing MIDI data with video
 * or other time-based media.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property hour the SMPTE hour component.
 * @property minute the SMPTE minute component.
 * @property second the SMPTE second component.
 * @property frame the SMPTE frame component.
 * @property subframe the SMPTE subframe component.
 */
data class SmpteOffsetMetaEvent(
  override val deltaTime: Int,
  val hour: Int,
  val minute: Int,
  val second: Int,
  val frame: Int,
  val subframe: Int
) : Event(deltaTime)

/**
 * Represents the Time Signature meta event (`FF 58`).
 *
 * This event describes the musical meter and provides timing information
 * used by MIDI sequencers and notation software.
 *
 * The denominator is represented in MIDI files as a power of two. For
 * example, a stored denominator value of `2` means:
 *
 * ```
 * 2^2 = 4
 * ```
 *
 * and therefore represents a denominator of `4`.
 *
 * [clocksPerTick] specifies the number of MIDI clocks per metronome click,
 * while [notesPer24Clocks] specifies the number of notated 32nd notes
 * represented by 24 MIDI clocks.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property numerator the numerator of the time signature.
 * @property denominator the exponent of two used to represent the denominator.
 * @property clocksPerTick the number of MIDI clocks per metronome click.
 * @property notesPer24Clocks the number of notated 32nd notes per 24 MIDI clocks.
 */
data class TimeSignatureMetaEvent(
  override val deltaTime: Int,
  val numerator: Int,
  val denominator: Int,
  val clocksPerTick: Int,
  val notesPer24Clocks: Int
) : Event(deltaTime)

/**
 * Represents the Key Signature meta event (`FF 59`).
 *
 * This event describes the key signature and whether it is major or minor.
 *
 * The [key] value is represented as a signed number of sharps or flats:
 *
 * - `0` = C major / A minor
 * - Positive values = number of sharps
 * - Negative values = number of flats
 *
 * The [scale] value identifies the mode:
 *
 * - `0` = major
 * - `1` = minor
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property key the number of sharps or flats, represented as a signed value.
 * @property scale the scale type, where `0` represents major and `1` represents
 * minor.
 */
data class KeySignatureMetaEvent(
  override val deltaTime: Int,
  val key: Int,
  val scale: Int
) : Event(deltaTime)

/**
 * Represents a Sequencer-Specific meta event (`FF 7F`).
 *
 * This event stores manufacturer- or sequencer-specific information that is
 * not defined by the standard MIDI meta-event types.
 *
 * The contents are intentionally kept as raw bytes because their meaning
 * depends on the software or hardware that created the event.
 *
 * For example, a DAW may store proprietary project information inside this
 * event that cannot be interpreted without knowing the corresponding format.
 *
 * The custom [equals] and [hashCode] implementations are necessary because
 * Kotlin's default equality for [ByteArray] is reference-based rather than
 * content-based.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property rawData the raw, uninterpreted event payload.
 */
data class SequencerSpecificMetaEvent(
  override val deltaTime: Int,
  val rawData: ByteArray
) : Event(deltaTime) {

  /**
   * Compares this event with another object using the contents of [rawData].
   *
   * Two events are considered equal when their delta-times and byte-array
   * contents are equal.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SequencerSpecificMetaEvent) return false

    return deltaTime == other.deltaTime &&
        rawData.contentEquals(other.rawData)
  }

  /**
   * Calculates a hash code based on the contents of [rawData].
   *
   * This keeps [hashCode] consistent with [equals], allowing instances to be
   * safely used in hash-based collections.
   */
  override fun hashCode(): Int {
    var result = deltaTime
    result = 31 * result + rawData.contentHashCode()
    return result
  }
}

/**
 * Represents a meta event whose type is not explicitly modeled by the library.
 *
 * This class allows the parser to preserve unknown or unsupported meta events
 * instead of discarding their contents.
 *
 * Keeping the original [type] and [rawData] is useful for:
 *
 * - inspecting MIDI files containing uncommon extensions;
 * - preserving information when rewriting a file;
 * - adding support for new event types later;
 * - debugging malformed or unusual MIDI files.
 *
 * The raw payload is intentionally kept uninterpreted.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property type the raw MIDI meta-event type byte.
 * @property rawData the raw payload associated with the event.
 */
data class UnknownMetaEvent(
  override val deltaTime: Int,
  val type: Int,
  val rawData: ByteArray
) : Event(deltaTime) {

  /**
   * Compares this event with another object using the contents of [rawData].
   *
   * Two events are equal when their delta-times, event types, and byte-array
   * contents are equal.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is UnknownMetaEvent) return false

    return deltaTime == other.deltaTime &&
        type == other.type &&
        rawData.contentEquals(other.rawData)
  }

  /**
   * Calculates a hash code based on the contents of [rawData].
   *
   * This keeps [hashCode] consistent with [equals].
   */
  override fun hashCode(): Int {
    var result = deltaTime
    result = 31 * result + type
    result = 31 * result + rawData.contentHashCode()
    return result
  }
}

// ---------------------------------------------------------
// CHANNEL EVENTS
// ---------------------------------------------------------

/**
 * Represents a MIDI Note On channel event.
 *
 * A Note On event starts a note on a specific MIDI channel. The [note] value
 * identifies the MIDI note number, while [velocity] represents how strongly
 * the note was triggered.
 *
 * A Note On event with velocity `0` is conventionally interpreted as a
 * Note Off event by MIDI devices.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property channel the MIDI channel, typically in the range `0..15`.
 * @property note the MIDI note number, typically in the range `0..127`.
 * @property velocity the note-on velocity, typically in the range `0..127`.
 */
data class NoteOnControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val note: Int,
  val velocity: Int
) : Event(deltaTime)

/**
 * Represents a MIDI Note Off channel event.
 *
 * A Note Off event terminates a previously started note on the specified
 * MIDI channel.
 *
 * The [velocity] field represents the release velocity when supported by
 * the MIDI device or synthesizer.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property channel the MIDI channel, typically in the range `0..15`.
 * @property note the MIDI note number, typically in the range `0..127`.
 * @property velocity the note-off velocity, typically in the range `0..127`.
 */
data class NoteOffControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val note: Int,
  val velocity: Int
) : Event(deltaTime)

/**
 * Represents a Polyphonic Key Pressure channel event.
 *
 * This event applies independent aftertouch pressure to a specific note.
 * Unlike Channel Pressure, which affects the entire channel, Polyphonic Key
 * Pressure identifies the affected note explicitly.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property channel the MIDI channel, typically in the range `0..15`.
 * @property note the MIDI note number, typically in the range `0..127`.
 * @property pressure the key pressure value, typically in the range `0..127`.
 */
data class PolyphonicKeyPressureControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val note: Int,
  val pressure: Int
) : Event(deltaTime)

/**
 * Represents a MIDI Control Change channel event.
 *
 * Control Change messages modify controller values associated with a MIDI
 * channel. Common controllers include modulation, volume, pan, sustain, and
 * many device-specific parameters.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property channel the MIDI channel, typically in the range `0..15`.
 * @property controller the controller number, typically in the range `0..127`.
 * @property value the new controller value, typically in the range `0..127`.
 */
data class ControlChangeControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val controller: Int,
  val value: Int
) : Event(deltaTime)

/**
 * Represents a MIDI Program Change channel event.
 *
 * Program Change selects a program, patch, or instrument sound on a MIDI
 * device.
 *
 * Unlike most channel messages, Program Change contains only one data byte,
 * so there is no separate value field.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property channel the MIDI channel, typically in the range `0..15`.
 * @property program the program number, typically in the range `0..127`.
 */
data class ProgramChangeControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val program: Int
) : Event(deltaTime)

/**
 * Represents a MIDI Channel Pressure channel event.
 *
 * Channel Pressure, also known as channel aftertouch, applies pressure
 * information to the entire MIDI channel rather than to an individual note.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property channel the MIDI channel, typically in the range `0..15`.
 * @property pressure the channel pressure value, typically in the range `0..127`.
 */
data class ChannelPressureControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val pressure: Int
) : Event(deltaTime)

/**
 * Represents a MIDI Pitch Bend channel event.
 *
 * Pitch Bend provides continuous pitch control for a MIDI channel.
 *
 * The MIDI protocol represents pitch bend using a 14-bit value assembled
 * from two data bytes. The logical center position is normally `8192`,
 * corresponding to no pitch deviation.
 *
 * Typical values are:
 *
 * ```
 * 0      -> maximum downward bend
 * 8192   -> center / no bend
 * 16383  -> maximum upward bend
 * ```
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property channel the MIDI channel, typically in the range `0..15`.
 * @property bend the decoded 14-bit pitch-bend value.
 */
data class PitchBendControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val bend: Int
) : Event(deltaTime)

// ---------------------------------------------------------
// SYSTEM EXCLUSIVE
// ---------------------------------------------------------

/**
 * Represents a MIDI System Exclusive (SysEx) event.
 *
 * SysEx messages are used for manufacturer-specific MIDI communication.
 * Unlike standard channel events, their payload is not interpreted according
 * to the normal channel-message structure.
 *
 * The raw bytes are therefore preserved in [rawData].
 *
 * The [escaped] flag indicates whether this event originated from a MIDI
 * System Exclusive escape event rather than a normal `F0` SysEx event.
 * This distinction matters when parsing and later reproducing the original
 * MIDI representation.
 *
 * The custom [equals] and [hashCode] implementations are necessary because
 * Kotlin's default equality for [ByteArray] is reference-based rather than
 * content-based.
 *
 * @property deltaTime the time elapsed since the previous event.
 * @property rawData the raw SysEx payload.
 * @property escaped whether this event represents a SysEx escape event
 * (`F7`) rather than a standard SysEx start event (`F0`).
 */
data class SysExEvent(
  override val deltaTime: Int,
  val rawData: ByteArray,
  val escaped: Boolean = false
) : Event(deltaTime) {

  /**
   * Compares this event with another object using the contents of [rawData].
   *
   * Two SysEx events are equal when their delta-times, raw-data contents,
   * and [escaped] states are equal.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SysExEvent) return false

    return deltaTime == other.deltaTime &&
        rawData.contentEquals(other.rawData) &&
        escaped == other.escaped
  }

  /**
   * Calculates a hash code based on the contents of [rawData] and the
   * [escaped] state.
   *
   * This keeps [hashCode] consistent with [equals].
   */
  override fun hashCode(): Int {
    var result = deltaTime
    result = 31 * result + rawData.contentHashCode()
    result = 31 * result + escaped.hashCode()
    return result
  }
}