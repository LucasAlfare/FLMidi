@file:Suppress("ArrayInDataClass")

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.hexDump

enum class EventType(val code: Int) {
  Meta(0xFF),
  SystemExclusive(0xF0),
  SystemExclusiveEscape(0xF7),
  Other(0)
}

enum class EventSubType(val code: Int) {
  // Meta Events
  SequenceNumberMetaEvent(0x00),
  TextEventMetaEvent(0x01),
  CopyrightNoticeMetaEvent(0x02),
  TrackNameMetaEvent(0x03),
  InstrumentNameMetaEvent(0x04),
  LyricMetaEvent(0x05),
  MarkerMetaEvent(0x06),
  CuePointMetaEvent(0x07),
  MidiChannelPrefixMetaEvent(0x20),
  SetTempoMetaEvent(0x51),
  SmpteOffsetMetaEvent(0x54),
  TimeSignatureMetaEvent(0x58),
  KeySignatureMetaEvent(0x59),
  SequencerSpecificMetaEvent(0x7F),
  EndOfTrackMetaEvent(0x2F),
  UnknownEventSubType(-1),

  // Control Events (high nibble)
  NoteOffControlEvent(0x8),
  NoteOnControlEvent(0x9),
  PolyphonicKeyPressureControlEvent(0xA),
  ControlChangeControlEvent(0xB),
  ProgramChangeControlEvent(0xC),
  ChannelPressureControlEvent(0xD),
  PitchBendControlEvent(0xE);

  companion object {
    fun fromCode(code: Int): EventSubType = entries.find { it.code == code } ?: UnknownEventSubType
  }
}

open class Event(
  val type: EventType,
  val subType: EventSubType,
  open val deltaTime: Int
)

data class SequenceNumberMetaEvent(
  override val deltaTime: Int,
  val sequenceNumber: Int
) : Event(
  type = EventType.Meta,
  subType = EventSubType.SequenceNumberMetaEvent,
  deltaTime = deltaTime
)

data class TextMetaEvent(
  override val deltaTime: Int,
  val text: String
) : Event(
  type = EventType.Meta,
  subType = EventSubType.TextEventMetaEvent,
  deltaTime = deltaTime
)

data class CopyrightNoticeMetaEvent(
  override val deltaTime: Int,
  val copyrightNotice: String
) : Event(
  type = EventType.Meta,
  subType = EventSubType.CopyrightNoticeMetaEvent,
  deltaTime = deltaTime
)

data class TrackNameMetaEvent(
  override val deltaTime: Int,
  val trackName: String
) : Event(
  type = EventType.Meta,
  subType = EventSubType.TrackNameMetaEvent,
  deltaTime = deltaTime
)

data class InstrumentNameMetaEvent(
  override val deltaTime: Int,
  val instrumentName: String
) : Event(
  type = EventType.Meta,
  subType = EventSubType.InstrumentNameMetaEvent,
  deltaTime = deltaTime
)

data class LyricMetaEvent(
  override val deltaTime: Int,
  val lyric: String
) : Event(
  type = EventType.Meta,
  subType = EventSubType.LyricMetaEvent,
  deltaTime = deltaTime
)

data class MarkerMetaEvent(
  override val deltaTime: Int,
  val marker: String
) : Event(
  type = EventType.Meta,
  subType = EventSubType.MarkerMetaEvent,
  deltaTime = deltaTime
)

data class CuePointMetaEvent(
  override val deltaTime: Int,
  val cuePoint: String
) : Event(
  type = EventType.Meta,
  subType = EventSubType.CuePointMetaEvent,
  deltaTime = deltaTime
)

data class MidiChannelPrefixMetaEvent(
  override val deltaTime: Int,
  val midiChannelPrefix: Int
) : Event(
  type = EventType.Meta,
  subType = EventSubType.MidiChannelPrefixMetaEvent,
  deltaTime = deltaTime
)

data class SetTempoMetaEvent(
  override val deltaTime: Int,
  val tempo: Int
) : Event(
  type = EventType.Meta,
  subType = EventSubType.SetTempoMetaEvent,
  deltaTime = deltaTime
)

data class SmpteOffsetMetaEvent(
  override val deltaTime: Int,
  val hour: Int,
  val minute: Int,
  val second: Int,
  val frame: Int,
  val subframe: Int
) : Event(
  type = EventType.Meta,
  subType = EventSubType.SmpteOffsetMetaEvent,
  deltaTime = deltaTime
)

data class TimeSignatureMetaEvent(
  override val deltaTime: Int,
  val numerator: Int,
  val denominator: Int,
  val clocksPerTick: Int,
  val notesPer24Clocks: Int
) : Event(
  type = EventType.Meta,
  subType = EventSubType.TimeSignatureMetaEvent,
  deltaTime = deltaTime
)

data class KeySignatureMetaEvent(
  override val deltaTime: Int,
  val key: Int,
  val scale: Int
) : Event(
  type = EventType.Meta,
  subType = EventSubType.KeySignatureMetaEvent,
  deltaTime = deltaTime
)

data class SequencerSpecificMetaEvent(
  override val deltaTime: Int,
  val rawData: ByteArray
) : Event(
  type = EventType.Meta,
  subType = EventSubType.SequencerSpecificMetaEvent,
  deltaTime = deltaTime
)

data class EndOfTrackMetaEvent(
  override val deltaTime: Int
) : Event(
  type = EventType.Meta,
  subType = EventSubType.EndOfTrackMetaEvent,
  deltaTime = deltaTime
)

data class UnknownMetaEvent(
  override val deltaTime: Int,
  val unknownRawData: ByteArray
) : Event(
  type = EventType.Meta,
  subType = EventSubType.UnknownEventSubType,
  deltaTime = deltaTime
)

data class NoteOnControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val note: Int,
  val velocity: Int
) : Event(
  type = EventType.Other,
  subType = EventSubType.NoteOnControlEvent,
  deltaTime = deltaTime
)

data class NoteOffControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val note: Int,
  val velocity: Int
) : Event(
  type = EventType.Other,
  subType = EventSubType.NoteOffControlEvent,
  deltaTime = deltaTime
)

data class PolyphonicKeyPressureControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val note: Int,
  val pressure: Int
) : Event(
  type = EventType.Other,
  subType = EventSubType.PolyphonicKeyPressureControlEvent,
  deltaTime = deltaTime
)

data class ControlChangeControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val controller: Int,
  val value: Int
) : Event(
  type = EventType.Other,
  subType = EventSubType.ControlChangeControlEvent,
  deltaTime = deltaTime
)

data class ProgramChangeControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val program: Int
) : Event(
  type = EventType.Other,
  subType = EventSubType.ProgramChangeControlEvent,
  deltaTime = deltaTime
)

data class ChannelPressureControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val pressure: Int
) : Event(
  type = EventType.Other,
  subType = EventSubType.ChannelPressureControlEvent,
  deltaTime = deltaTime
)

data class PitchBendControlEvent(
  override val deltaTime: Int,
  val channel: Int,
  val bend: Int
) : Event(
  type = EventType.Other,
  subType = EventSubType.PitchBendControlEvent,
  deltaTime = deltaTime
)

data class SysExEvent(
  override val deltaTime: Int,
  val sysexRawData: ByteArray
) : Event(
  type = EventType.SystemExclusive,
  subType = EventSubType.UnknownEventSubType,
  deltaTime = deltaTime
)

data class Header(
  val signature: String,
  val length: Long,
  val format: Int,
  val numTracks: Int,
  val division: Int
)

data class Track(
  val signature: String,
  val length: Int,
  val events: List<Event>
) {
  val name: String = events.filterIsInstance<TrackNameMetaEvent>().singleOrNull()?.trackName ?: ""
}

@OptIn(ExperimentalUnsignedTypes::class)
data class Midi(
  val header: Header,
  val tracks: List<Track>,
  val rawBytes: UByteArray = UByteArray(0)
) {
  fun dumpedRawBytes(bytesPerLine: Int = 16): String =
    hexDump(data = rawBytes, bytesPerLine = bytesPerLine)
}