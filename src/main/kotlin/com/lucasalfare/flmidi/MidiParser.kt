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
  val code = reader.read1Byte()
  val metaType = MetaEventType.fromCode(code)
  val length = reader.readVariableLengthValue()

  return when (metaType) {
    MetaEventType.SequenceNumber -> {
      val sequenceNumber = reader.read2Bytes()
      SequenceNumberMetaEvent(deltaTime = deltaTime, sequenceNumber = sequenceNumber)
    }

    MetaEventType.TextEvent ->
      TextMetaEvent(deltaTime = deltaTime, text = reader.readString(length))

    MetaEventType.CopyrightNotice ->
      CopyrightNoticeMetaEvent(deltaTime = deltaTime, copyrightNotice = reader.readString(length))

    MetaEventType.TrackName ->
      TrackNameMetaEvent(deltaTime = deltaTime, trackName = reader.readString(length))

    MetaEventType.InstrumentName ->
      InstrumentNameMetaEvent(deltaTime = deltaTime, instrumentName = reader.readString(length))

    MetaEventType.Lyric ->
      LyricMetaEvent(deltaTime = deltaTime, lyric = reader.readString(length))

    MetaEventType.Marker ->
      MarkerMetaEvent(deltaTime = deltaTime, marker = reader.readString(length))

    MetaEventType.CuePoint ->
      CuePointMetaEvent(deltaTime = deltaTime, cuePoint = reader.readString(length))

    MetaEventType.MidiChannelPrefix -> {
      val channel = reader.read1Byte()
      MidiChannelPrefixMetaEvent(deltaTime = deltaTime, midiChannelPrefix = channel)
    }

    MetaEventType.SetTempo -> {
      val tempo = reader.read3Bytes()
      SetTempoMetaEvent(deltaTime = deltaTime, tempo = tempo)
    }

    MetaEventType.SmpteOffset -> {
      val hour = reader.read1Byte()
      val minute = reader.read1Byte()
      val second = reader.read1Byte()
      val frame = reader.read1Byte()
      val subframe = reader.read1Byte()
      SmpteOffsetMetaEvent(
        deltaTime = deltaTime,
        hour = hour,
        minute = minute,
        second = second,
        frame = frame,
        subframe = subframe
      )
    }

    MetaEventType.TimeSignature -> {
      val numerator = reader.read1Byte()
      val denominator = 2f.pow(reader.read1Byte().toFloat()).toInt()
      val clocksPerTick = reader.read1Byte()
      val notesPer24Clocks = reader.read1Byte()
      TimeSignatureMetaEvent(
        deltaTime = deltaTime,
        numerator = numerator,
        denominator = denominator,
        clocksPerTick = clocksPerTick,
        notesPer24Clocks = notesPer24Clocks
      )
    }

    MetaEventType.KeySignature -> {
      val key = reader.read1Byte()
      val scale = reader.read1Byte()
      KeySignatureMetaEvent(deltaTime = deltaTime, key = key, scale = scale)
    }

    MetaEventType.SequencerSpecific -> {
      val data = ByteArray(length) { reader.read1Byte().toByte() }
      SequencerSpecificMetaEvent(deltaTime = deltaTime, rawData = data)
    }

    MetaEventType.EndOfTrack -> {
      require(length == 0) { "EndOfTrack event should have zero length" }
      EndOfTrackMetaEvent(deltaTime = deltaTime)
    }

    MetaEventType.Unknown -> {
      val data = ByteArray(length) { reader.read1Byte().toByte() }
      UnknownMetaEvent(deltaTime = deltaTime, unknownRawData = data)
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
  val channel = status and 0x0F
  val controlCode = status shr 4
  val controlType = ControlEventType.fromCode(controlCode)

  return when (controlType) {
    ControlEventType.NoteOn -> {
      val note = reader.read1Byte()
      val velocity = reader.read1Byte()
      NoteOnControlEvent(deltaTime = deltaTime, channel = channel, note = note, velocity = velocity)
    }

    ControlEventType.NoteOff -> {
      val note = reader.read1Byte()
      val velocity = reader.read1Byte()
      NoteOffControlEvent(deltaTime = deltaTime, channel = channel, note = note, velocity = velocity)
    }

    ControlEventType.PolyphonicKeyPressure -> {
      val note = reader.read1Byte()
      val pressure = reader.read1Byte()
      PolyphonicKeyPressureControlEvent(
        deltaTime = deltaTime,
        channel = channel,
        note = note,
        pressure = pressure
      )
    }

    ControlEventType.ControlChange -> {
      val controller = reader.read1Byte()
      val value = reader.read1Byte()
      ControlChangeControlEvent(
        deltaTime = deltaTime,
        channel = channel,
        controller = controller,
        value = value
      )
    }

    ControlEventType.ProgramChange -> {
      val program = reader.read1Byte()
      ProgramChangeControlEvent(deltaTime = deltaTime, channel = channel, program = program)
    }

    ControlEventType.ChannelPressure -> {
      val pressure = reader.read1Byte()
      ChannelPressureControlEvent(deltaTime = deltaTime, channel = channel, pressure = pressure)
    }

    ControlEventType.PitchBend -> {
      val lsb = reader.read1Byte()
      val msb = reader.read1Byte()
      val bend = (msb shl 7) or lsb
      PitchBendControlEvent(deltaTime = deltaTime, channel = channel, bend = bend)
    }
  }
}

fun readMidiFromBytes(midiBytes: ByteArray): Midi {
  val unsignedBytes = midiBytes.toUByteArray()
  val reader = Reader(unsignedBytes)

  // Read the header
  val header = Header(
    signature = reader.readString(4),
    length = reader.read4Bytes(),
    format = reader.read2Bytes(),
    numTracks = reader.read2Bytes(),
    division = reader.read2Bytes()
  )

  // Read tracks
  val tracks = mutableListOf<Track>()
  repeat(header.numTracks) {
    val trackType = reader.readString(4)
    val trackLength = reader.read4Bytes().toInt()
    val finalOffset = reader.position + trackLength
    val events = mutableListOf<Event>()
    var previousStatus = 0

    while (reader.position < finalOffset) {
      val deltaTime = reader.readVariableLengthValue()
      var status = reader.read1Byte()

      // Suporte ao "running status"
      if (status ushr 7 == 0) {
        status = previousStatus
        reader.position--
      }

      when (status) {
        EventType.Meta.code -> {
          val metaEvent = readMetaEvent(reader, deltaTime)
          events += metaEvent
          if (metaEvent.eventType == MetaEventType.EndOfTrack) break
          previousStatus = 0
        }

        EventType.SystemExclusive.code, EventType.SystemExclusiveEscape.code -> {
          val length = reader.readVariableLengthValue()
          val data = ByteArray(length) { reader.read1Byte().toByte() }
          events += SysExEvent(deltaTime = deltaTime, sysexRawData = data)
          previousStatus = 0
        }

        else -> {
          previousStatus = status
          val controlEvent = readControlEvent(reader, deltaTime, status)
          events += controlEvent
        }
      }
    }

    tracks += Track(signature = trackType, length = trackLength, events = events)
  }

  return Midi(header = header, tracks = tracks, rawBytes = unsignedBytes)
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
fun readMidiFromFile(pathname: String): Midi {
  val file = File(pathname)
  require(file.exists()) { "File does not exist" }
  require(!file.isDirectory) { "Path [$pathname] is a directory, not a file" }
  val fileBytes = file.readBytes()
  return readMidiFromBytes(fileBytes)
}