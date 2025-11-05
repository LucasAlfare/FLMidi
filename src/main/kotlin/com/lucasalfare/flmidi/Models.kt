package com.lucasalfare.flmidi

import kotlinx.serialization.Serializable

@Serializable
enum class EventType(val code: Int) {
  Meta(0xFF),

  SystemExclusive(0xF0),

  SystemExclusiveEscape(0xF7);

  companion object {
    fun fromCode(code: Int): EventType? = entries.find { it.code == code }
  }
}

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

@Serializable
open class Event

@Serializable
open class MetaEvent : Event()

@Serializable
open class ControlEvent() : Event()

open class SysExEvent(val deltaTime: Int, data: ByteArray) : Event()

@Serializable
data class SequenceNumberMetaEvent(val metaEventType: Int, val deltaTime: Int, val sequenceNumber: Int) : MetaEvent()

@Serializable
data class TextEventMetaEvent(val metaEventType: Int, val deltaTime: Int, val textData: String) : MetaEvent()

@Serializable
data class CopyrightNoticeMetaEvent(val metaEventType: Int, val deltaTime: Int, val textData: String) : MetaEvent()

@Serializable
data class TrackNameMetaEvent(val metaEventType: Int, val deltaTime: Int, val textData: String) : MetaEvent()

@Serializable
data class InstrumentNameMetaEvent(val metaEventType: Int, val deltaTime: Int, val textData: String) : MetaEvent()

@Serializable
data class LyricMetaEvent(val metaEventType: Int, val deltaTime: Int, val textData: String) : MetaEvent()

@Serializable
data class MarkerMetaEvent(val metaEventType: Int, val deltaTime: Int, val textData: String) : MetaEvent()

@Serializable
data class CuePointMetaEvent(val metaEventType: Int, val deltaTime: Int, val textData: String) : MetaEvent()

@Serializable
data class TimeSignatureMetaEvent(
  val metaEventType: Int,
  val deltaTime: Int,
  val upperSignature: Int,
  val powerOfTwoToLowerValue: Int,
  val nMidiClocksInMetronomeClick: Int,
  val nMidiClocksOf32ndNotesIn24MidiClocks: Int
) : MetaEvent()

@Serializable
data class SetTempoMetaEvent(val metaEventType: Int, val deltaTime: Int, val tempoInMicroseconds: Int) : MetaEvent()

@Serializable
data class SmpteOffsetMetaEvent(
  val metaEventType: Int,
  val deltaTime: Int,
  val byte1: Int,
  val byte2: Int,
  val byte3: Int,
  val byte4: Int,
  val byte5: Int
) : MetaEvent()

@Serializable
data class KeySignatureMetaEvent(
  val metaEventType: Int,
  val deltaTime: Int,
  val byte1: Int,
  val byte2: Int
) : MetaEvent()

@Serializable
data class MidiChannelPrefixMetaEvent(
  val metaEventType: Int,
  val deltaTime: Int,
  val currentEffectiveMidiChannel: Int
) : MetaEvent()

@Serializable
data class SequencerSpecificMetaEvent(val metaEventType: Int, val deltaTime: Int, val bytes: List<Int>) : MetaEvent()

@Serializable
data class EndOfTrackMetaEvent(val metaEventType: Int, val deltaTime: Int, val bytes: List<Int>) : MetaEvent()

@Serializable
data class UnkownMetaEvent(val metaEventType: Int, val deltaTime: Int, val bytes: List<Int>) : MetaEvent()

@Serializable
data class ProgramChangeControlEvent(
  val controlEventType: Int,
  val deltaTime: Int,
  val targetChannel: Int,
  val targetInstrument: Int
) : ControlEvent()

@Serializable
data class NoteOnControlEvent(
  val controlEventType: Int,
  val deltaTime: Int,
  val targetChannel: Int,
  val noteNumber: Int,
  val noteVelocity: Int
) : ControlEvent()

@Serializable
data class NoteOffControlEvent(
  val controlEventType: Int,
  val deltaTime: Int,
  val targetChannel: Int,
  val noteNumber: Int,
  val noteVelocity: Int
) : ControlEvent()

@Serializable
data class PolyphonicKeyPressureControlEvent(
  val controlEventType: Int,
  val deltaTime: Int,
  val targetChannel: Int,
  val noteNumber: Int,
  val pressure: Int
) : ControlEvent()

@Serializable
data class ControlChangeControlEvent(
  val controlEventType: Int,
  val deltaTime: Int,
  val targetChannel: Int,
  val controlNumber: Int,
  val controlValue: Int
) : ControlEvent()

@Serializable
data class ChannelPressureControlEvent(
  val controlEventType: Int,
  val deltaTime: Int,
  val targetChannel: Int,
  val channelPressure: Int
) : ControlEvent()

@Serializable
data class PitchBendControlEvent(
  val controlEventType: Int,
  val deltaTime: Int,
  val targetChannel: Int,
  val pitchBend: Int
) : ControlEvent()

@Serializable
data class Header(
  val chunkType: String,
  val length: Long,
  val format: Int,
  val numTracks: Int,
  val division: Int
) {
  init {
    require(chunkType == "MThd") { "Header chunk type signature is not 'MThd'!" }
    if (format == 0) require(numTracks == 1) { "Format 0 MIDI files must contain exactly one track!" }
    else if (format == 1 || format == 2) require(numTracks >= 1) { "MIDI file must contain at least one track!" }
  }
}

@Serializable
data class Track(
  val type: String,
  val length: Int,
  val events: List<Event>
) {
  init {
    require(type == "MTrk") { "Track type signature is not 'MTrk'!" }
    require(length > 0) { "Track with length 0!" }
    require(events.isNotEmpty()) { "Track without any events!" }
  }
}

@Serializable
data class Midi(
  val header: Header,
  val tracks: List<Track>
)