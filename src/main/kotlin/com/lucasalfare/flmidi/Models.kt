@file:Suppress("ArrayInDataClass")

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.hexDump
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents the kind of the event is.
 */
@Serializable
enum class EventType(val code: Int) {
  Meta(0xFF),
  SystemExclusive(0xF0),
  SystemExclusiveEscape(0xF7);

  companion object {
    fun fromCode(code: Int): EventType? = entries.find { it.code == code }
  }
}

/**
 * All possible MIDI Meta Event type is represented by one of these entries.
 */
@Serializable
enum class MetaEventType(val code: Int) {
  SequenceNumber(0x00),
  TextEvent(0x01),
  CopyrightNotice(0x02),
  TrackName(0x03),
  InstrumentName(0x04),
  Lyric(0x05),
  Marker(0x06),
  CuePoint(0x07),
  MidiChannelPrefix(0x20),
  SetTempo(0x51),
  SmpteOffset(0x54),
  TimeSignature(0x58),
  KeySignature(0x59),
  SequencerSpecific(0x7F),
  EndOfTrack(0x2F),
  Unknown(-1);

  companion object {
    fun fromCode(code: Int): MetaEventType = entries.find { it.code == code } ?: Unknown
  }

  override fun toString() = "${this.name}(0x${code.toString(16).uppercase().padStart(2, '0')})"
}

/**
 * All possible MIDI Control Event type is represented by one of these entries.
 */
@Serializable
enum class ControlEventType(val code: Int) {
  NoteOff(0b1000),
  NoteOn(0b1001),
  PolyphonicKeyPressure(0b1010),
  ControlChange(0b1011),
  ProgramChange(0b1100),
  ChannelPressure(0b1101),
  PitchBend(0b1110);

  companion object {
    fun fromCode(code: Int): ControlEventType =
      entries.find { it.code == code } ?: throw IllegalArgumentException(
        "Unknown control event type: ${code.toString(16)}"
      )
  }

  override fun toString() = "${this.name}(0b${code.toString(2).padStart(4, '0')})"
}

/**
 * Base class to represent an abstract Event entity.
 * All events have a [deltaTime] associated.
 */
@Serializable
sealed class Event {
  abstract val deltaTime: Int
}

/**
 * Abstract class to represent any kind of Meta Event.
 * We keep track of an [eventType] to flag this with the desired type.
 */
@Serializable
sealed class MetaEvent : Event() {
  abstract val eventType: MetaEventType
}

/**
 * Class to represent a Control Event.
 * We keep track the type and also the [channel].
 */
@Serializable
sealed class ControlEvent : Event() {
  abstract val eventType: ControlEventType
  abstract val channel: Int
  val statusByte: Int get() = (eventType.code shl 4) or (channel and 0x0F)
}

@Serializable
@SerialName("sequence_number")
data class SequenceNumberMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.SequenceNumber,
  val sequenceNumber: Int
) : MetaEvent()

@Serializable
@SerialName("text")
data class TextMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.TextEvent,
  val text: String
) : MetaEvent()

@Serializable
@SerialName("copyright_notice")
data class CopyrightNoticeMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.CopyrightNotice,
  val copyrightNotice: String
) : MetaEvent()

@Serializable
@SerialName("track_name")
data class TrackNameMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.TrackName,
  val trackName: String
) : MetaEvent()

@Serializable
@SerialName("instrument_name")
data class InstrumentNameMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.InstrumentName,
  val instrumentName: String
) : MetaEvent()

@Serializable
@SerialName("lyric")
data class LyricMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.Lyric,
  val lyric: String
) : MetaEvent()

@Serializable
@SerialName("marker")
data class MarkerMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.Marker,
  val marker: String
) : MetaEvent()

@Serializable
@SerialName("cue_point")
data class CuePointMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.CuePoint,
  val cuePoint: String
) : MetaEvent()

@Serializable
@SerialName("midi_channel_prefix")
data class MidiChannelPrefixMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.MidiChannelPrefix,
  val midiChannelPrefix: Int
) : MetaEvent()

@Serializable
@SerialName("set_tempo")
data class SetTempoMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.SetTempo,
  val tempo: Int
) : MetaEvent()

@Serializable
@SerialName("smpte_offset")
data class SmpteOffsetMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.SmpteOffset,
  val hour: Int,
  val minute: Int,
  val second: Int,
  val frame: Int,
  val subframe: Int
) : MetaEvent()

@Serializable
@SerialName("time_signature")
data class TimeSignatureMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.TimeSignature,
  val numerator: Int,
  val denominator: Int,
  val clocksPerTick: Int,
  val notesPer24Clocks: Int
) : MetaEvent()

@Serializable
@SerialName("key_signature")
data class KeySignatureMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.KeySignature,
  val key: Int,
  val scale: Int
) : MetaEvent()

@Serializable
@SerialName("sequencer_specific")
data class SequencerSpecificMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.SequencerSpecific,
  val rawData: ByteArray
) : MetaEvent()

@Serializable
@SerialName("end_of_track")
data class EndOfTrackMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.EndOfTrack
) : MetaEvent()

@Serializable
@SerialName("unknown_meta")
data class UnknownMetaEvent(
  override val deltaTime: Int,
  override val eventType: MetaEventType = MetaEventType.Unknown,
  val unknownRawData: ByteArray
) : MetaEvent()

@Serializable
@SerialName("note_on")
data class NoteOnControlEvent(
  override val eventType: ControlEventType = ControlEventType.NoteOn,
  override val deltaTime: Int,
  override val channel: Int,
  val note: Int,
  val velocity: Int
) : ControlEvent()

@Serializable
@SerialName("note_off")
data class NoteOffControlEvent(
  override val eventType: ControlEventType = ControlEventType.NoteOff,
  override val deltaTime: Int,
  override val channel: Int,
  val note: Int,
  val velocity: Int
) : ControlEvent()

@Serializable
@SerialName("polyphonic_key_pressure")
data class PolyphonicKeyPressureControlEvent(
  override val eventType: ControlEventType = ControlEventType.PolyphonicKeyPressure,
  override val deltaTime: Int,
  override val channel: Int,
  val note: Int,
  val pressure: Int
) : ControlEvent()

@Serializable
@SerialName("control_change")
data class ControlChangeControlEvent(
  override val eventType: ControlEventType = ControlEventType.ControlChange,
  override val deltaTime: Int,
  override val channel: Int,
  val controller: Int,
  val value: Int
) : ControlEvent()

@Serializable
@SerialName("program_change")
data class ProgramChangeControlEvent(
  override val eventType: ControlEventType = ControlEventType.ProgramChange,
  override val deltaTime: Int,
  override val channel: Int,
  val program: Int
) : ControlEvent()

@Serializable
@SerialName("channel_pressure")
data class ChannelPressureControlEvent(
  override val eventType: ControlEventType = ControlEventType.ChannelPressure,
  override val deltaTime: Int,
  override val channel: Int,
  val pressure: Int
) : ControlEvent()

@Serializable
@SerialName("pitch_bend")
data class PitchBendControlEvent(
  override val eventType: ControlEventType = ControlEventType.PitchBend,
  override val deltaTime: Int,
  override val channel: Int,
  val bend: Int
) : ControlEvent()

@Serializable
@SerialName("sysex")
data class SysExEvent(
  override val deltaTime: Int,
  val sysexRawData: ByteArray
) : Event()

@Serializable
data class Header(
  val signature: String,
  val length: Long,
  val format: Int,
  val numTracks: Int,
  val division: Int
)

@Serializable
data class Track(
  val signature: String,
  val length: Int,
  val events: List<Event>
) {

  /**
   * Helper field to retrieve track name!
   *
   * If track doesn't have a [TrackNameMetaEvent], just leaves it empty.
   */
  val name: String = events.filterIsInstance<TrackNameMetaEvent>().singleOrNull()?.trackName ?: ""
}

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class Midi(
  val header: Header,
  val tracks: List<Track>,
  @Transient val rawBytes: UByteArray = UByteArray(0)
) {

  fun dumpedRawBytes(bytesPerLine: Int = 16): String {
    return hexDump(data = rawBytes, bytesPerLine = bytesPerLine)
  }
}