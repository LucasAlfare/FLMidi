package com.lucasalfare.flmidi

import kotlinx.serialization.Serializable

/**
 * Enumeration representing the high-level MIDI event categories.
 *
 * @property code The byte code associated with this event type.
 */
@Serializable
enum class EventType(val code: Int) {
  /**
   * Meta event indicator (always 0xFF).
   */
  Meta(0xFF),

  /**
   * System Exclusive event start indicator (always 0xF0).
   */
  SystemExclusive(0xF0),

  /**
   * System Exclusive event escape indicator (always 0xF7).
   */
  SystemExclusiveEscape(0xF7);

  companion object {
    /**
     * Retrieves the corresponding [EventType] for the given code.
     *
     * @param code The code to lookup.
     * @return The matching [EventType] if found, otherwise null.
     */
    fun fromCode(code: Int): EventType? = entries.find { it.code == code }
  }
}

/**
 * Enumeration representing the types of meta events in a MIDI file.
 *
 * @property code The byte code associated with this meta event type.
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

  /**
   * Represents an unknown meta event type.
   */
  Unknown(-1);

  companion object {
    /**
     * Retrieves the corresponding [MetaEventType] for the given code.
     *
     * @param code The code to lookup.
     * @return The matching [MetaEventType] if found; otherwise, [Unknown].
     */
    fun fromCode(code: Int): MetaEventType = entries.find { it.code == code } ?: Unknown
  }

  override fun toString() = "${this}(0x${code.toString(16).padStart(2, '0')})"
}

/**
 * Enumeration representing the types of MIDI control (channel) events.
 *
 * @property code The 4-bit code associated with this control event type.
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
    /**
     * Retrieves the corresponding [ControlEventType] for the given 4-bit code.
     *
     * @param code The 4-bit code to lookup.
     * @return The matching [ControlEventType].
     * @throws IllegalArgumentException If the code does not correspond to a known control event.
     */
    fun fromCode(code: Int): ControlEventType =
      entries.find { it.code == code } ?: throw IllegalArgumentException(
        "Unknown control event type: ${code.toString(16)}"
      )
  }

  override fun toString() = "${this}(0x${code.toString(16).padStart(2, '0')})"
}

@Serializable
sealed class EventData

@Serializable
data class NumberEventData(
  val number: Int
) : EventData()

@Serializable
data class TextEventData(
  val text: String
) : EventData()

@Serializable
data class NumberListEventData(
  val list: List<Int>
) : EventData()

/**
 * Base class for all MIDI events.
 */
@Serializable
sealed class Event

/**
 * Data class representing a meta event in a MIDI file.
 *
 * @property eventType The type of meta event (e.g., [MetaEventType.TextEvent], [MetaEventType.SetTempo]).
 * @property deltaTime The delta time before this event occurs.
 * @property data The data payload for this event; its type depends on the event.
 */
@Serializable
data class MetaEvent(
  val eventType: MetaEventType,
  val deltaTime: Int,
  val data: EventData
) : Event()

/**
 * Data class representing a control (channel) event in a MIDI file.
 *
 * @property eventType The type of control event (e.g., [ControlEventType.NoteOn], [ControlEventType.ControlChange]).
 * @property delta The delta time before this event occurs.
 * @property data The data payload for this event.
 * @property targetChannel The MIDI channel associated with this event.
 */
@Serializable
data class ControlEvent(
  val eventType: ControlEventType,
  val delta: Int,
  val data: EventData,
  val targetChannel: Int
) : Event()

/**
 * Data class representing the header chunk of a MIDI file.
 *
 * @property chunkType The signature of the header chunk (must be "MThd").
 * @property length The length of the header chunk in bytes.
 * @property format The format type of the MIDI file (0, 1, or 2).
 * @property numTracks The number of track chunks in the MIDI file.
 * @property division The time division (ticks per quarter note or SMPTE format).
 *
 * The initializer validates that the header signature is correct and that the number
 * of tracks is appropriate for the given format.
 */
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

/**
 * Data class representing a track chunk in a MIDI file.
 *
 * @property type The signature of the track chunk (must be "MTrk").
 * @property length The length of the track chunk in bytes.
 * @property events The list of MIDI events contained in the track.
 *
 * The initializer validates that the track signature is correct, the length is positive,
 * and that there is at least one event.
 */
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

/**
 * Data class representing an entire MIDI file.
 *
 * @property header The header chunk of the MIDI file.
 * @property tracks The list of track chunks in the MIDI file.
 */
@Serializable
data class Midi(
  val header: Header,
  val tracks: List<Track>
)