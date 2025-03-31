@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.math.pow

/**
 * General MIDI event types and constants.
 *
 * These constants represent the different types of events in a MIDI file.
 * The format follows the Standard MIDI File (SMF) specification.
 */

/** Meta event type indicator (always 0xFF). */
const val META_EVENT = 0xFF

/** System Exclusive event start indicator (always 0xF0). */
const val SYSTEM_EXCLUSIVE_EVENT = 0xF0

/** System Exclusive event escape indicator (always 0xF7). */
const val SYSTEM_EXCLUSIVE_ESCAPE_EVENT = 0xF7

/**
 * Meta-event type constants.
 *
 * Each constant represents a specific type of meta-event, which are used to store
 * non-MIDI performance data such as tempo, time signature, and textual information.
 */

/** Meta event: Sequence Number (0x00). */
const val SEQUENCE_NUMBER = 0x00

/** Meta event: Text Event (0x01). */
const val TEXT_EVENT = 0x01

/** Meta event: Copyright Notice (0x02). */
const val COPYRIGHT_NOTICE = 0x02

/** Meta event: Track Name (0x03). */
const val TRACK_NAME = 0x03

/** Meta event: Instrument Name (0x04). */
const val INSTRUMENT_NAME = 0x04

/** Meta event: Lyric (0x05). */
const val LYRIC = 0x05

/** Meta event: Marker (0x06). */
const val MARKER = 0x06

/** Meta event: Cue Point (0x07). */
const val CUE_POINT = 0x07

/** Meta event: MIDI Channel Prefix (0x20). */
const val MIDI_CHANNEL_PREFIX = 0x20

/** Meta event: End of Track (0x2F). */
const val END_OF_TRACK = 0x2F

/** Meta event: Set Tempo (0x51). */
const val SET_TEMPO = 0x51

/** Meta event: SMPTE Offset (0x54). */
const val SMPTE_OFFSET = 0x54

/** Meta event: Time Signature (0x58). */
const val TIME_SIGNATURE = 0x58

/** Meta event: Key Signature (0x59). */
const val KEY_SIGNATURE = 0x59

/** Meta event: Sequencer Specific (0x7F). */
const val SEQUENCER_SPECIFIC = 0x7F

/**
 * Control event type constants.
 *
 * These constants use only the 4 most significant bits from the status byte.
 * They represent various MIDI performance messages.
 */

/** Control event: Note Off (0b1000). */
const val NOTE_OFF = 0b1000

/** Control event: Note On (0b1001). */
const val NOTE_ON = 0b1001

/** Control event: Polyphonic Key Pressure (0b1010). */
const val POLYPHONIC_KEY_PRESSURE = 0b1010

/** Control event: Control Change (0b1011). */
const val CONTROL_CHANGE = 0b1011

/** Control event: Program Change (0b1100). */
const val PROGRAM_CHANGE = 0b1100

/** Control event: Channel Pressure (0b1101). */
const val CHANNEL_PRESSURE = 0b1101

/** Control event: Pitch Bend (0b1110). */
const val PITCH_BEND = 0b1110

/**
 * Base class for all MIDI events.
 */
open class Event

/**
 * Data class representing a meta event in a MIDI file.
 *
 * @property eventType The type of meta event (e.g., TEXT_EVENT, SET_TEMPO).
 * @property deltaTime The delta time before this event occurs.
 * @property data The data payload for this event, its type depends on the event.
 */
data class MetaEvent(
  val eventType: Int,
  val deltaTime: Int,
  val data: Any
) : Event()

/**
 * Data class representing a control event (channel event) in a MIDI file.
 *
 * @property type The type of control event (e.g., NOTE_ON, CONTROL_CHANGE).
 * @property delta The delta time before this event occurs.
 * @property data The data payload for this event.
 * @property targetChannel The MIDI channel this event is associated with.
 */
data class ControlEvent(
  val type: Int,
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
 * The initializer validates that the header signature is correct and
 * that the number of tracks is appropriate for the given format.
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
    if (format == 0) require(numTracks == 1)
    else if (format == 1 || format == 2) require(numTracks >= 1)
  }
}

/**
 * Data class representing a track chunk in a MIDI file.
 *
 * @property type The signature of the track chunk (must be "MTrk").
 * @property length The length of the track chunk in bytes.
 * @property events The list of MIDI events contained in the track.
 *
 * The initializer validates that the track signature is correct,
 * the length is positive, and that there is at least one event.
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
 * the event type, delta time, and associated data. Unknown meta events are also handled.
 *
 * @param reader The [Reader] instance used to read bytes from the MIDI file.
 * @param deltaTime The delta time preceding the event.
 * @return The parsed [MetaEvent] object.
 */
private fun readMetaEvent(reader: Reader, deltaTime: Int): MetaEvent {
  return when (val type = reader.read1Byte()) {
    SEQUENCE_NUMBER -> {
      val dataLength = reader.readVariableLengthValue()
      val sequenceNumber = reader.read2Bytes()
      MetaEvent(type, deltaTime, sequenceNumber)
    }

    TEXT_EVENT, COPYRIGHT_NOTICE, TRACK_NAME, INSTRUMENT_NAME, LYRIC, MARKER, CUE_POINT -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      MetaEvent(type, deltaTime, data)
    }

    TIME_SIGNATURE -> {
      val numDataItems = reader.readVariableLengthValue()
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
      MetaEvent(type, deltaTime, data)
    }

    SET_TEMPO -> {
      val numDataItems = reader.readVariableLengthValue()
      val tempoInMicroseconds = reader.read3Bytes()
      MetaEvent(type, deltaTime, tempoInMicroseconds)
    }

    SMPTE_OFFSET -> {
      val dataLength = reader.readVariableLengthValue()
      val data = listOf(
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte()
      )
      MetaEvent(type, deltaTime, data)
    }

    KEY_SIGNATURE -> {
      val dataLength = reader.readVariableLengthValue()
      val data = listOf(reader.read1Byte(), reader.read1Byte())
      MetaEvent(type, deltaTime, data)
    }

    MIDI_CHANNEL_PREFIX -> {
      val dataLength = reader.readVariableLengthValue()
      val currentEffectiveMidiChannel = reader.read1Byte()
      MetaEvent(type, deltaTime, currentEffectiveMidiChannel)
    }

    SEQUENCER_SPECIFIC -> {
      val dataLength = reader.readVariableLengthValue()
      val auxBytes = mutableListOf<Int>()
      repeat(dataLength) { auxBytes += reader.read1Byte() }
      MetaEvent(type, deltaTime, auxBytes)
    }

    END_OF_TRACK -> {
      reader.readVariableLengthValue().also { require(it == 0) }
      MetaEvent(type, deltaTime, emptyList<Int>())
    }

    else -> {
      println("Unknown meta event encountered: [$type]. Reading anyway...")
      val dataLength = reader.readVariableLengthValue()
      repeat(dataLength) { reader.read1Byte() }
      MetaEvent(type, deltaTime, emptyList<Int>())
    }
  }
}

/**
 * Reads a control event (channel event) from the provided [reader] using the given [deltaTime] and [status].
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
  val channel = status and 0b1111
  return when (val type = status shr 4) {
    PROGRAM_CHANGE -> {
      val targetInstrument = reader.read1Byte()
      ControlEvent(type, deltaTime, targetInstrument, channel)
    }

    NOTE_ON, NOTE_OFF -> {
      val noteNumber = reader.read1Byte() and 0b01111111
      val noteVelocity = reader.read1Byte() and 0b01111111
      val data = listOf(noteNumber, noteVelocity)
      ControlEvent(type, deltaTime, data, channel)
    }

    POLYPHONIC_KEY_PRESSURE -> {
      val noteNumber = reader.read1Byte()
      val pressure = reader.read1Byte()
      val data = listOf(noteNumber, pressure)
      ControlEvent(type, deltaTime, data, channel)
    }

    CONTROL_CHANGE -> {
      val controlNumber = reader.read1Byte()
      val controlValue = reader.read1Byte()
      val data = listOf(controlNumber, controlValue)
      ControlEvent(type, deltaTime, data, channel)
    }

    CHANNEL_PRESSURE -> {
      val channelPressure = reader.read1Byte()
      ControlEvent(type, deltaTime, channelPressure, channel)
    }

    PITCH_BEND -> {
      val lsb = reader.read1Byte()
      val msb = reader.read1Byte()
      val pitchBend = (msb shl 7) or lsb
      ControlEvent(type, deltaTime, pitchBend, channel)
    }

    else -> error("Unknown control event type: ${type.toString(16)}")
  }
}

/**
 * Reads and parses an entire MIDI file from the given [pathname].
 *
 * The function validates that the file exists and is not a directory,
 * then reads the header chunk and each track chunk, parsing all contained events.
 *
 * @param pathname The file path to the MIDI file.
 * @return A [Midi] object containing the header and all track events.
 * @throws IllegalArgumentException If the file does not exist or if the path points to a directory.
 */
fun readMidi(pathname: String): Midi {
  val file = File(pathname)
  if (!file.exists()) error("File does not exist")
  if (file.isDirectory) error("Path [$pathname] is a directory, not a file")

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

      // Support for "running status"
      if (currentStatus ushr 7 == 0) {
        currentStatus = previousStatus
        reader.position--
      }

      when (currentStatus) {
        META_EVENT -> {
          val metaEvent = readMetaEvent(reader, currentDeltaTime)
          events += metaEvent
          if (metaEvent.eventType == END_OF_TRACK) break
          previousStatus = 0 // Reset after meta-event
        }

        SYSTEM_EXCLUSIVE_EVENT, SYSTEM_EXCLUSIVE_ESCAPE_EVENT -> {
          val length = reader.readVariableLengthValue()
          repeat(length) { reader.read1Byte() }
          println("SysEx event found! Skipping $length byte(s)...")
          previousStatus = 0 // Reset after SysEx
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

fun main() {
  val midi = readMidi("example4.mid")
  midi.tracks.first().events.forEach { println(it) }
}