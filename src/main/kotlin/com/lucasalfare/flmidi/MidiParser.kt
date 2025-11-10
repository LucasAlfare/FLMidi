@file:OptIn(ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.math.*

fun readMetaEvent(reader: Reader, deltaTime: Int): Event? {
  val code = reader.read1Byte()
  val metaType = EventSubType.fromCode(code)
  val length = reader.readVariableLengthValue()

  return when (metaType) {
    EventSubType.SequenceNumberMetaEvent -> {
      // Sequence number should be 2 bytes, but we still read according to length to be robust
      val sequenceNumber = when (length) {
        2 -> reader.read2Bytes()
        else -> {
          // If length != 2, try to read up to 2 bytes or default 0
          val bytes = ByteArray(length) { reader.read1Byte().toByte() }
          if (bytes.size >= 2) ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF) else 0
        }
      }
      SequenceNumberMetaEvent(deltaTime = deltaTime, sequenceNumber = sequenceNumber)
    }

    EventSubType.TextEventMetaEvent ->
      TextMetaEvent(deltaTime = deltaTime, text = reader.readString(length))

    EventSubType.CopyrightNoticeMetaEvent ->
      CopyrightNoticeMetaEvent(deltaTime = deltaTime, copyrightNotice = reader.readString(length))

    EventSubType.TrackNameMetaEvent ->
      TrackNameMetaEvent(deltaTime = deltaTime, trackName = reader.readString(length))

    EventSubType.InstrumentNameMetaEvent ->
      InstrumentNameMetaEvent(deltaTime = deltaTime, instrumentName = reader.readString(length))

    EventSubType.LyricMetaEvent ->
      LyricMetaEvent(deltaTime = deltaTime, lyric = reader.readString(length))

    EventSubType.MarkerMetaEvent ->
      MarkerMetaEvent(deltaTime = deltaTime, marker = reader.readString(length))

    EventSubType.CuePointMetaEvent ->
      CuePointMetaEvent(deltaTime = deltaTime, cuePoint = reader.readString(length))

    EventSubType.MidiChannelPrefixMetaEvent -> {
      val channel = reader.read1Byte()
      MidiChannelPrefixMetaEvent(deltaTime = deltaTime, midiChannelPrefix = channel)
    }

    EventSubType.SetTempoMetaEvent -> {
      val tempo = reader.read3Bytes()
      SetTempoMetaEvent(deltaTime = deltaTime, tempo = tempo)
    }

    EventSubType.SmpteOffsetMetaEvent -> {
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

    EventSubType.TimeSignatureMetaEvent -> {
      val numerator = reader.read1Byte()
      val denominatorExp = reader.read1Byte()
      val denominator = 1 shl denominatorExp // 2^denominatorExp
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

    EventSubType.KeySignatureMetaEvent -> {
      val key = reader.read1Byte()
      val scale = reader.read1Byte()
      KeySignatureMetaEvent(deltaTime = deltaTime, key = key, scale = scale)
    }

    EventSubType.SequencerSpecificMetaEvent -> {
      val data = ByteArray(length) { reader.read1Byte().toByte() }
      SequencerSpecificMetaEvent(deltaTime = deltaTime, rawData = data)
    }

    EventSubType.EndOfTrackMetaEvent -> {
      require(length == 0) { "EndOfTrack event should have zero length" }
      EndOfTrackMetaEvent(deltaTime = deltaTime)
    }

    EventSubType.UnknownEventSubType -> {
      val data = ByteArray(length) { reader.read1Byte().toByte() }
      UnknownMetaEvent(deltaTime = deltaTime, unknownRawData = data)
    }

    else -> null
  }
}

fun readControlEvent(reader: Reader, deltaTime: Int, status: Int): Event? {
  val channel = status and 0x0F
  val controlCode = status shr 4
  val controlType = EventSubType.fromCode(controlCode)

  return when (controlType) {
    EventSubType.NoteOnControlEvent -> {
      val note = reader.read1Byte()
      val velocity = reader.read1Byte()
      NoteOnControlEvent(deltaTime = deltaTime, channel = channel, note = note, velocity = velocity)
    }

    EventSubType.NoteOffControlEvent -> {
      val note = reader.read1Byte()
      val velocity = reader.read1Byte()
      NoteOffControlEvent(deltaTime = deltaTime, channel = channel, note = note, velocity = velocity)
    }

    EventSubType.PolyphonicKeyPressureControlEvent -> {
      val note = reader.read1Byte()
      val pressure = reader.read1Byte()
      PolyphonicKeyPressureControlEvent(
        deltaTime = deltaTime,
        channel = channel,
        note = note,
        pressure = pressure
      )
    }

    EventSubType.ControlChangeControlEvent -> {
      val controller = reader.read1Byte()
      val value = reader.read1Byte()
      ControlChangeControlEvent(
        deltaTime = deltaTime,
        channel = channel,
        controller = controller,
        value = value
      )
    }

    EventSubType.ProgramChangeControlEvent -> {
      val program = reader.read1Byte()
      ProgramChangeControlEvent(deltaTime = deltaTime, channel = channel, program = program)
    }

    EventSubType.ChannelPressureControlEvent -> {
      val pressure = reader.read1Byte()
      ChannelPressureControlEvent(deltaTime = deltaTime, channel = channel, pressure = pressure)
    }

    EventSubType.PitchBendControlEvent -> {
      val lsb = reader.read1Byte()
      val msb = reader.read1Byte()
      val bend = (msb shl 7) or lsb
      PitchBendControlEvent(deltaTime = deltaTime, channel = channel, bend = bend)
    }

    else -> null
  }
}

fun readMidiFromBytes(midiBytes: ByteArray): Midi {
  val unsignedBytes = midiBytes.toUByteArray()
  val reader = Reader(unsignedBytes)

  // Read header
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

      // Running status: if byte < 0x80 it's a data byte, so reuse previousStatus and step back
      if (status ushr 7 == 0) {
        status = previousStatus
        reader.position--
      }

      when (status) {
        EventType.Meta.code -> {
          val metaEvent = readMetaEvent(reader, deltaTime)
          if (metaEvent != null) {
            events += metaEvent
            if (metaEvent.subType == EventSubType.EndOfTrackMetaEvent) break
            previousStatus = 0
          }
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
          if (controlEvent != null) events += controlEvent
        }
      }
    }

    tracks += Track(signature = trackType, length = trackLength, events = events)
  }

  return Midi(header = header, tracks = tracks, rawBytes = unsignedBytes)
}

fun readMidiFromFile(pathname: String): Midi {
  val file = File(pathname)
  require(file.exists()) { "File does not exist" }
  require(!file.isDirectory) { "Path [$pathname] is a directory, not a file" }
  val fileBytes = file.readBytes()
  return readMidiFromBytes(fileBytes)
}
