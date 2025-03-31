@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.math.pow

/**
 * Enumeration representing the high-level MIDI event categories.
 *
 * @property code The byte code associated with this event type.
 */
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
}

/**
 * Enumeration representing the types of MIDI control (channel) events.
 *
 * @property code The 4-bit code associated with this control event type.
 */
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
}

/**
 * Base class for all MIDI events.
 */
open class Event

/**
 * Data class representing a meta event in a MIDI file.
 *
 * @property type The type of meta event (e.g., [MetaEventType.TextEvent], [MetaEventType.SetTempo]).
 * @property deltaTime The delta time before this event occurs.
 * @property data The data payload for this event; its type depends on the event.
 */
data class MetaEvent(
  val type: MetaEventType,
  val deltaTime: Int,
  val data: Any
) : Event()

/**
 * Data class representing a control (channel) event in a MIDI file.
 *
 * @property type The type of control event (e.g., [ControlEventType.NoteOn], [ControlEventType.ControlChange]).
 * @property delta The delta time before this event occurs.
 * @property data The data payload for this event.
 * @property targetChannel The MIDI channel associated with this event.
 */
data class ControlEvent(
  val type: ControlEventType,
  val delta: Int,
  val data: Any,
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
data class Midi(
  val header: Header,
  val tracks: List<Track>
)

/**
 * Reads a meta event from the provided [reader] using the given [deltaTime].
 *
 * The function handles various meta event types and returns a [MetaEvent] containing
 * the event type, delta time, and associated data. Unknown meta events are handled by
 * assigning the [MetaEventType.Unknown] type.
 *
 * @param reader The [Reader] instance used to read bytes from the MIDI file.
 * @param deltaTime The delta time preceding the event.
 * @return The parsed [MetaEvent] object.
 */
private fun readMetaEvent(reader: Reader, deltaTime: Int): MetaEvent {
  // Read the meta event type code and resolve it to the corresponding enum value.
  val code = reader.read1Byte()
  return when (val metaType = MetaEventType.fromCode(code)) {
    MetaEventType.SequenceNumber -> {
      reader.readVariableLengthValue() // data length (usually fixed)
      val sequenceNumber = reader.read2Bytes()
      MetaEvent(metaType, deltaTime, sequenceNumber)
    }

    MetaEventType.TextEvent,
    MetaEventType.CopyrightNotice,
    MetaEventType.TrackName,
    MetaEventType.InstrumentName,
    MetaEventType.Lyric,
    MetaEventType.Marker,
    MetaEventType.CuePoint -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      MetaEvent(metaType, deltaTime, data)
    }

    MetaEventType.TimeSignature -> {
      reader.readVariableLengthValue() // number of data items (should be 4)
      val upperSignatureValue = reader.read1Byte()
      val powerOfTwoToLowerValue = reader.read1Byte()
      val nMidiClocksInMetronomeClick = reader.read1Byte()
      val numberOf32ndNotesIn24MidiClocks = reader.read1Byte()
      val data = listOf(
        upperSignatureValue,
        2f.pow(powerOfTwoToLowerValue).toInt(),
        nMidiClocksInMetronomeClick,
        numberOf32ndNotesIn24MidiClocks
      )
      MetaEvent(metaType, deltaTime, data)
    }

    MetaEventType.SetTempo -> {
      reader.readVariableLengthValue() // number of data items (should be 3)
      val tempoInMicroseconds = reader.read3Bytes()
      MetaEvent(metaType, deltaTime, tempoInMicroseconds)
    }

    MetaEventType.SmpteOffset -> {
      reader.readVariableLengthValue() // data length (should be 5)
      val data = listOf(
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte()
      )
      MetaEvent(metaType, deltaTime, data)
    }

    MetaEventType.KeySignature -> {
      reader.readVariableLengthValue() // data length (should be 2)
      val data = listOf(reader.read1Byte(), reader.read1Byte())
      MetaEvent(metaType, deltaTime, data)
    }

    MetaEventType.MidiChannelPrefix -> {
      reader.readVariableLengthValue() // data length (should be 1)
      val currentEffectiveMidiChannel = reader.read1Byte()
      MetaEvent(metaType, deltaTime, currentEffectiveMidiChannel)
    }

    MetaEventType.SequencerSpecific -> {
      val dataLength = reader.readVariableLengthValue()
      val auxBytes = mutableListOf<Int>()
      repeat(dataLength) { auxBytes += reader.read1Byte() }
      MetaEvent(metaType, deltaTime, auxBytes)
    }

    MetaEventType.EndOfTrack -> {
      val dataLength = reader.readVariableLengthValue()
      require(dataLength == 0) { "End of Track meta event should have zero data length." }
      MetaEvent(metaType, deltaTime, emptyList<Int>())
    }

    MetaEventType.Unknown -> {
      println("Unknown meta event encountered: [0x${code.toString(16)}]. Reading anyway...")
      val dataLength = reader.readVariableLengthValue()
      repeat(dataLength) { reader.read1Byte() }
      MetaEvent(metaType, deltaTime, emptyList<Int>())
    }
  }
}

/**
 * Reads a control (channel) event from the provided [reader] using the given [deltaTime] and [status].
 *
 * This function decodes various types of control events (e.g., NOTE_ON, PROGRAM_CHANGE) based on the status byte.
 * It supports handling of running status.
 *
 * @param reader The [Reader] instance used to read bytes from the MIDI file.
 * @param deltaTime The delta time preceding the event.
 * @param status The status byte of the event.
 * @return The parsed [ControlEvent] object.
 * @throws IllegalArgumentException If an unknown control event type is encountered.
 */
private fun readControlEvent(reader: Reader, deltaTime: Int, status: Int): ControlEvent {
  // Extract channel from the status byte (lower 4 bits)
  val channel = status and 0b1111
  // Extract control event type from the status byte (upper 4 bits)
  val controlCode = status shr 4
  return when (val controlType = ControlEventType.fromCode(controlCode)) {
    ControlEventType.ProgramChange -> {
      val targetInstrument = reader.read1Byte()
      ControlEvent(controlType, deltaTime, targetInstrument, channel)
    }

    ControlEventType.NoteOn, ControlEventType.NoteOff -> {
      val noteNumber = reader.read1Byte() and 0b01111111
      val noteVelocity = reader.read1Byte() and 0b01111111
      val data = listOf(noteNumber, noteVelocity)
      ControlEvent(controlType, deltaTime, data, channel)
    }

    ControlEventType.PolyphonicKeyPressure -> {
      val noteNumber = reader.read1Byte()
      val pressure = reader.read1Byte()
      val data = listOf(noteNumber, pressure)
      ControlEvent(controlType, deltaTime, data, channel)
    }

    ControlEventType.ControlChange -> {
      val controlNumber = reader.read1Byte()
      val controlValue = reader.read1Byte()
      val data = listOf(controlNumber, controlValue)
      ControlEvent(controlType, deltaTime, data, channel)
    }

    ControlEventType.ChannelPressure -> {
      val channelPressure = reader.read1Byte()
      ControlEvent(controlType, deltaTime, channelPressure, channel)
    }

    ControlEventType.PitchBend -> {
      val lsb = reader.read1Byte()
      val msb = reader.read1Byte()
      val pitchBend = (msb shl 7) or lsb
      ControlEvent(controlType, deltaTime, pitchBend, channel)
    }
  }
}

/**
 * Reads and parses an entire MIDI file from the given [pathname].
 *
 * The function validates that the file exists and is not a directory, then reads the header chunk
 * and each track chunk, parsing all contained events.
 *
 * @param pathname The file path to the MIDI file.
 * @return A [Midi] object containing the header and all track events.
 * @throws IllegalArgumentException If the file does not exist or if the path points to a directory.
 */
fun readMidi(pathname: String): Midi {
  val file = File(pathname)
  require(file.exists()) { "File does not exist" }
  require(!file.isDirectory) { "Path [$pathname] is a directory, not a file" }

  val fileBytes = file.readBytes().toUByteArray()
  val reader = Reader(fileBytes)

  // Read the header chunk
  val header = Header(
    chunkType = reader.readString(4) ?: error("Missing header chunk type signature!"),
    length = reader.read4Bytes(),
    format = reader.read2Bytes(),
    numTracks = reader.read2Bytes(),
    division = reader.read2Bytes()
  )

  // Read track chunks
  val tracks = mutableListOf<Track>()
  repeat(header.numTracks) {
    val trackType = reader.readString(4) ?: error("Missing track chunk type signature!")
    val trackLength = reader.read4Bytes().toInt()
    val finalOffset = reader.position + trackLength
    val events = mutableListOf<Event>()
    var previousStatus = 0

    while (reader.position < finalOffset) {
      val currentDeltaTime = reader.readVariableLengthValue()
      var currentStatus = reader.read1Byte()

      // Support for "running status" where the status byte is omitted.
      if (currentStatus ushr 7 == 0) {
        currentStatus = previousStatus
        reader.position--
      }

      when (currentStatus) {
        EventType.Meta.code -> {
          val metaEvent = readMetaEvent(reader, currentDeltaTime)
          events += metaEvent
          if (metaEvent.type == MetaEventType.EndOfTrack) break
          previousStatus = 0 // Reset running status after a meta event.
        }

        EventType.SystemExclusive.code, EventType.SystemExclusiveEscape.code -> {
          val length = reader.readVariableLengthValue()
          repeat(length) { reader.read1Byte() }
          println("SysEx event found! Skipping $length byte(s)...")
          previousStatus = 0 // Reset running status after SysEx events.
        }

        else -> {
          previousStatus = currentStatus
          val controlEvent = readControlEvent(reader, currentDeltaTime, currentStatus)
          events += controlEvent
        }
      }
    }

    tracks += Track(type = trackType, length = trackLength, events = events)
  }

  return Midi(header = header, tracks = tracks)
}