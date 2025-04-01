@file:OptIn(ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.math.pow

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
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "Int($sequenceNumber)")
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
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "String($data)")
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
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "NumberList($data)")
    }

    MetaEventType.SetTempo -> {
      reader.readVariableLengthValue() // number of data items (should be 3)
      val tempoInMicroseconds = reader.read3Bytes()
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "Int($tempoInMicroseconds)")
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
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "NumberList($data)")
    }

    MetaEventType.KeySignature -> {
      reader.readVariableLengthValue() // data length (should be 2)
      val data = listOf(reader.read1Byte(), reader.read1Byte())
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "NumberList($data)")
    }

    MetaEventType.MidiChannelPrefix -> {
      reader.readVariableLengthValue() // data length (should be 1)
      val currentEffectiveMidiChannel = reader.read1Byte()
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "Int($currentEffectiveMidiChannel)")
    }

    MetaEventType.SequencerSpecific -> {
      val dataLength = reader.readVariableLengthValue()
      val auxBytes = mutableListOf<Int>()
      repeat(dataLength) { auxBytes += reader.read1Byte() }
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "NumberList($auxBytes)")
    }

    MetaEventType.EndOfTrack -> {
      val dataLength = reader.readVariableLengthValue()
      require(dataLength == 0) { "End of Track meta event should have zero data length." }
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "NumberList(${emptyList<Int>()})")
    }

    MetaEventType.Unknown -> {
      println("Unknown meta event encountered: [0x${code.toString(16)}]. Reading anyway...")
      val dataLength = reader.readVariableLengthValue()
      repeat(dataLength) { reader.read1Byte() }
      MetaEvent(eventType = metaType, deltaTime = deltaTime, data = "NumberList(${emptyList<Int>()})")
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
      ControlEvent(eventType = controlType, delta = deltaTime, data = "Int($targetInstrument)", targetChannel = channel)
    }

    ControlEventType.NoteOn, ControlEventType.NoteOff -> {
      val noteNumber = reader.read1Byte() and 0b01111111
      val noteVelocity = reader.read1Byte() and 0b01111111
      val data = listOf(noteNumber, noteVelocity)
      ControlEvent(eventType = controlType, delta = deltaTime, data = "NumberList($data)", targetChannel = channel)
    }

    ControlEventType.PolyphonicKeyPressure -> {
      val noteNumber = reader.read1Byte()
      val pressure = reader.read1Byte()
      val data = listOf(noteNumber, pressure)
      ControlEvent(eventType = controlType, delta = deltaTime, data = "NumberList($data)", targetChannel = channel)
    }

    ControlEventType.ControlChange -> {
      val controlNumber = reader.read1Byte()
      val controlValue = reader.read1Byte()
      val data = listOf(controlNumber, controlValue)
      ControlEvent(eventType = controlType, delta = deltaTime, data = "NumberList($data)", targetChannel = channel)
    }

    ControlEventType.ChannelPressure -> {
      val channelPressure = reader.read1Byte()
      ControlEvent(eventType = controlType, delta = deltaTime, data = "Int($channelPressure)", targetChannel = channel)
    }

    ControlEventType.PitchBend -> {
      val lsb = reader.read1Byte()
      val msb = reader.read1Byte()
      val pitchBend = (msb shl 7) or lsb
      ControlEvent(eventType = controlType, delta = deltaTime, data = "Int($pitchBend)", targetChannel = channel)
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
          if (metaEvent.eventType == MetaEventType.EndOfTrack) break
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