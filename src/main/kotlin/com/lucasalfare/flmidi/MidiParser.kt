package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.collections.plusAssign
import kotlin.math.pow

fun readMetaEvent(reader: Reader, deltaTime: Int): MetaEvent {
  // Read the meta event type code and resolve it to the corresponding enum value.
  val code = reader.read1Byte()
  return when (val metaType = MetaEventType.fromCode(code)) {
    MetaEventType.SequenceNumber -> {
      reader.readVariableLengthValue() // data length (usually fixed)
      val sequenceNumber = reader.read2Bytes()
      SequenceNumberMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, sequenceNumber = sequenceNumber)
    }

    MetaEventType.TextEvent -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      TextEventMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, textData = data)
    }

    MetaEventType.CopyrightNotice -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      CopyrightNoticeMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, textData = data)
    }

    MetaEventType.TrackName -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      TrackNameMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, textData = data)
    }

    MetaEventType.InstrumentName -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      InstrumentNameMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, textData = data)
    }

    MetaEventType.Lyric -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      LyricMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, textData = data)
    }

    MetaEventType.Marker -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      MarkerMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, textData = data)
    }

    MetaEventType.CuePoint -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      CuePointMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, textData = data)
    }

    MetaEventType.MidiChannelPrefix -> {
      reader.readVariableLengthValue() // data length (should be 1)
      val currentEffectiveMidiChannel = reader.read1Byte()
      MidiChannelPrefixMetaEvent(
        metaEventType = metaType.code,
        deltaTime = deltaTime,
        currentEffectiveMidiChannel = currentEffectiveMidiChannel
      )
    }

    MetaEventType.SetTempo -> {
      reader.readVariableLengthValue() // number of data items (should be 3)
      val tempoInMicroseconds = reader.read3Bytes()
      SetTempoMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, tempoInMicroseconds = tempoInMicroseconds)
    }

    MetaEventType.SmpteOffset -> {
      reader.readVariableLengthValue() // data length (should be 5)
      SmpteOffsetMetaEvent(
        metaEventType = metaType.code,
        deltaTime = deltaTime,
        byte1 = reader.read1Byte(),
        byte2 = reader.read1Byte(),
        byte3 = reader.read1Byte(),
        byte4 = reader.read1Byte(),
        byte5 = reader.read1Byte(),
      )
    }

    MetaEventType.TimeSignature -> {
      reader.readVariableLengthValue() // number of data items (should be 4)
      val upperSignatureValue = reader.read1Byte()
      val powerOfTwoToLowerValue = reader.read1Byte()
      val nMidiClocksInMetronomeClick = reader.read1Byte()
      val numberOf32ndNotesIn24MidiClocks = reader.read1Byte()
      TimeSignatureMetaEvent(
        metaEventType = metaType.code,
        deltaTime = deltaTime,
        upperSignature = upperSignatureValue,
        powerOfTwoToLowerValue = 2f.pow(powerOfTwoToLowerValue).toInt(),
        nMidiClocksInMetronomeClick = nMidiClocksInMetronomeClick,
        nMidiClocksOf32ndNotesIn24MidiClocks = numberOf32ndNotesIn24MidiClocks
      )
    }

    MetaEventType.KeySignature -> {
      reader.readVariableLengthValue() // data length (should be 2)
      KeySignatureMetaEvent(
        metaEventType = metaType.code,
        deltaTime = deltaTime,
        byte1 = reader.read1Byte(),
        byte2 = reader.read1Byte()
      )
    }

    MetaEventType.SequencerSpecific -> {
      val dataLength = reader.readVariableLengthValue()
      val auxBytes = mutableListOf<Int>()
      repeat(dataLength) { auxBytes += reader.read1Byte() }
      SequencerSpecificMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, bytes = auxBytes)
    }

    MetaEventType.EndOfTrack -> {
      val dataLength = reader.readVariableLengthValue()
      require(dataLength == 0) { "End of Track meta event should have zero data length." }
      EndOfTrackMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, bytes = emptyList())
    }

    MetaEventType.Unknown -> {
      println("Unknown meta event encountered: [0x${code.toString(16)}]. Reading anyway...")
      val dataLength = reader.readVariableLengthValue()
      repeat(dataLength) { reader.read1Byte() }
      UnkownMetaEvent(metaEventType = metaType.code, deltaTime = deltaTime, bytes = emptyList())
    }
  }
}

fun readControlEvent(reader: Reader, deltaTime: Int, status: Int): ControlEvent {
  // Extract channel from the status byte (lower 4 bits)
  val channel = status and 0b1111
  // Extract control event type from the status byte (upper 4 bits)
  val controlCode = status shr 4
  return when (val controlType = ControlEventType.fromCode(controlCode)) {
    ControlEventType.NoteOn -> {
      val noteNumber = reader.read1Byte() and 0b01111111
      val noteVelocity = reader.read1Byte() and 0b01111111
      NoteOnControlEvent(
        controlEventType = controlType.code,
        deltaTime = deltaTime,
        targetChannel = channel,
        noteNumber = noteNumber,
        noteVelocity = noteVelocity
      )
    }

    ControlEventType.NoteOff -> {
      val noteNumber = reader.read1Byte() and 0b01111111
      val noteVelocity = reader.read1Byte() and 0b01111111
      NoteOffControlEvent(
        controlEventType = controlType.code,
        deltaTime = deltaTime,
        targetChannel = channel,
        noteNumber = noteNumber,
        noteVelocity = noteVelocity
      )
    }

    ControlEventType.PolyphonicKeyPressure -> {
      val noteNumber = reader.read1Byte()
      val pressure = reader.read1Byte()
      PolyphonicKeyPressureControlEvent(
        controlEventType = controlType.code,
        deltaTime = deltaTime,
        targetChannel = channel,
        noteNumber = noteNumber,
        pressure = pressure
      )
    }

    ControlEventType.ControlChange -> {
      val controlNumber = reader.read1Byte()
      val controlValue = reader.read1Byte()
      ControlChangeControlEvent(
        controlEventType = controlType.code,
        deltaTime = deltaTime,
        targetChannel = channel,
        controlNumber = controlNumber,
        controlValue = controlValue
      )
    }

    ControlEventType.ProgramChange -> {
      val targetInstrument = reader.read1Byte()
      ProgramChangeControlEvent(
        controlEventType = controlType.code,
        deltaTime = deltaTime,
        targetChannel = channel,
        targetInstrument = targetInstrument
      )
    }

    ControlEventType.ChannelPressure -> {
      val channelPressure = reader.read1Byte()
      ChannelPressureControlEvent(
        controlEventType = controlType.code,
        deltaTime = deltaTime,
        targetChannel = channel,
        channelPressure = channelPressure
      )
    }

    ControlEventType.PitchBend -> {
      val lsb = reader.read1Byte()
      val msb = reader.read1Byte()
      val pitchBend = (msb shl 7) or lsb
      PitchBendControlEvent(
        controlEventType = controlType.code,
        deltaTime = deltaTime,
        targetChannel = channel,
        pitchBend = pitchBend
      )
    }
  }
}

@ExperimentalUnsignedTypes
fun readMidiFromBytes(midiBytes: ByteArray): Midi {
  val unsignedBytes = midiBytes.toUByteArray()
  val reader = Reader(unsignedBytes)

  // Read the header
  val header = Header(
    chunkType = reader.readString(4) ?: "",
    length = reader.read4Bytes(),
    format = reader.read2Bytes(),
    numTracks = reader.read2Bytes(),
    division = reader.read2Bytes()
  )

  // Read tracks
  val tracks = mutableListOf<Track>()
  repeat(header.numTracks) {
    val trackType = reader.readString(4) ?: ""
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

          if (metaEvent is EndOfTrackMetaEvent) break

//          if (metaEvent.eventType == MetaEventType.EndOfTrack) break
          previousStatus = 0
        }
        EventType.SystemExclusive.code, EventType.SystemExclusiveEscape.code -> {
          val length = reader.readVariableLengthValue()
          val data = ByteArray(length) { reader.read1Byte().toByte() }
          events += SysExEvent(deltaTime = deltaTime, data = data)
          previousStatus = 0
        }

        else -> {
          previousStatus = status
          val controlEvent = readControlEvent(reader, deltaTime, status)
          events += controlEvent
        }
      }
    }

    tracks += Track(type = trackType, length = trackLength, events = events)
  }

  return Midi(header = header, tracks = tracks)
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
@ExperimentalUnsignedTypes
fun readMidiFromFile(pathname: String): Midi {
  val file = File(pathname)
  require(file.exists()) { "File does not exist" }
  require(!file.isDirectory) { "Path [$pathname] is a directory, not a file" }
  val fileBytes = file.readBytes()
  return readMidiFromBytes(fileBytes)
}

@ExperimentalUnsignedTypes
fun main() {
  val midi = readMidiFromFile("example.mid")
  println(midi.header)
  midi.tracks.forEach {
    println("\tA new track:")
    it.events.forEach { e ->
      println("\t\t${e}")
    }
  }
}