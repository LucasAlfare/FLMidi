// MidiReader.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File

/**
 * Parses Standard MIDI Files (SMF) into the library's high-level [Midi] model.
 *
 * This object is responsible for converting the binary representation of a
 * MIDI file into structured Kotlin objects such as [Midi], [Event],
 * channel events, meta events, and System Exclusive events.
 *
 * The reader understands:
 *
 * - SMF headers (`MThd`);
 * - MIDI track chunks (`MTrk`);
 * - MIDI Variable-Length Quantities (VLQs);
 * - MIDI channel voice messages;
 * - MIDI running status;
 * - MIDI meta events;
 * - System Exclusive events;
 * - unknown meta events, which are preserved as raw data;
 * - track boundaries and End of Track events.
 *
 * The parser operates directly on [Reader], allowing callers to parse data
 * that is already available in memory without requiring a temporary file.
 *
 * The main entry points are:
 *
 * ```
 * MidiReader.fromFile(...)
 * MidiReader.fromBytes(...)
 * MidiReader.fromReader(...)
 * ```
 *
 * All three eventually use [fromReader], which contains the actual SMF
 * parsing logic.
 */
object MidiReader {

  /**
   * Reads and parses a MIDI file from the given filesystem path.
   *
   * The entire file is loaded into memory before being parsed.
   *
   * ### Example
   *
   * ```
   * val midi = MidiReader.fromFile("song.mid")
   * ```
   *
   * @param path path to the MIDI file.
   * @return the parsed [Midi] representation.
   *
   * @throws java.io.IOException if the file cannot be read.
   * @throws IllegalArgumentException if the file does not contain a valid
   * supported SMF structure.
   */
  fun fromFile(path: String): Midi {
    return fromBytes(File(path).readBytes())
  }

  /**
   * Parses a MIDI file from its complete binary representation.
   *
   * This is useful when MIDI data is already available as a [ByteArray],
   * such as data received from a network, loaded from another API, or
   * generated dynamically.
   *
   * ### Example
   *
   * ```
   * val bytes = java.io.File("song.mid").readBytes()
   * val midi = MidiReader.fromBytes(bytes)
   * ```
   *
   * @param bytes complete SMF binary data.
   * @return the parsed [Midi] representation.
   */
  fun fromBytes(bytes: ByteArray): Midi {
    return fromReader(Reader(bytes.toUByteArray()))
  }

  /**
   * Parses a Standard MIDI File from an existing [Reader].
   *
   * This is the main parsing entry point used internally by [fromFile] and
   * [fromBytes].
   *
   * The method first reads the SMF header:
   *
   * ```
   * MThd
   * ├── header length
   * ├── format
   * ├── number of tracks
   * └── division
   * ```
   *
   * It then reads the specified number of `MTrk` chunks and converts each
   * track into a list of [Event] objects.
   *
   * ### Header validation
   *
   * The SMF header must begin with:
   *
   * ```
   * 4D 54 68 64
   * ```
   *
   * which is the ASCII representation of `"MThd"`.
   *
   * The standard header data length is always six bytes:
   *
   * ```
   * format          -> 2 bytes
   * number of tracks -> 2 bytes
   * division         -> 2 bytes
   * ```
   *
   * ### Example
   *
   * ```
   * val reader = Reader(bytes.toUByteArray())
   * val midi = MidiReader.fromReader(reader)
   * ```
   *
   * @param reader binary reader positioned at the beginning of an SMF.
   * @return the parsed [Midi] representation.
   *
   * @throws IllegalArgumentException if the header, format, track chunks,
   * or track boundaries are invalid.
   */
  fun fromReader(reader: Reader): Midi {

    // ---------------------------------------------------------
    // HEADER CHUNK
    // ---------------------------------------------------------

    // The first four bytes of every SMF must be the ASCII signature "MThd".
    val headerSignature = reader.readString(4)

    require(headerSignature == "MThd") {
      "Invalid MIDI header signature: $headerSignature"
    }

    // The SMF header data length must be exactly six bytes.
    //
    // It does not include the four-byte "MThd" signature or the four-byte
    // length field itself.
    val headerLength = reader.read4Bytes()

    require(headerLength == 6L) {
      "Invalid MIDI header length: $headerLength"
    }

    val format = reader.read2Bytes()
    val numberOfTracks = reader.read2Bytes()
    val division = reader.read2Bytes()

    // Standard MIDI Files currently define formats 0, 1, and 2.
    require(format in 0..2) {
      "Unsupported MIDI format: $format"
    }

    // Format 0 normally contains exactly one track.
    // This validation is intentionally left optional here.

    // ---------------------------------------------------------
    // TRACK CHUNKS
    // ---------------------------------------------------------

    val tracks = mutableListOf<List<Event>>()

    repeat(numberOfTracks) {

      // Every track chunk must begin with "MTrk".
      val trackSignature = reader.readString(4)

      require(trackSignature == "MTrk") {
        "Invalid MIDI track signature: $trackSignature"
      }

      // The length describes only the track data that follows.
      val trackLength = reader.read4Bytes()

      require(trackLength >= 0) {
        "Invalid MIDI track length: $trackLength"
      }

      // Calculate the absolute reader position where this track ends.
      //
      // Keeping this boundary is important because MIDI events are variable
      // in size. Without an explicit end position, malformed event lengths
      // could cause the parser to consume bytes belonging to the next chunk.
      val trackEnd = reader.position + trackLength.toInt()

      require(trackEnd <= reader.data.size) {
        "MIDI track exceeds available data"
      }

      tracks += readTrack(reader, trackEnd)
    }

    return Midi(
      format = format,
      division = division,
      tracks = tracks
    )
  }

  /**
   * Reads all events contained within a single MIDI track.
   *
   * A track is a sequence of events, each encoded as:
   *
   * ```
   * delta-time
   * event
   * ```
   *
   * Delta-time values are MIDI Variable-Length Quantities and represent the
   * number of ticks elapsed since the previous event.
   *
   * This method also implements MIDI running status.
   *
   * With normal status bytes:
   *
   * ```
   * 90 3C 64
   * 90 40 64
   * ```
   *
   * the second `90` can be omitted when running status is active:
   *
   * ```
   * 90 3C 64
   * 40 64
   * ```
   *
   * A byte whose most significant bit is clear is therefore interpreted as
   * the first data byte of the currently active running-status message.
   *
   * Meta events and System Exclusive events reset running status.
   *
   * @param reader reader positioned at the beginning of the track data.
   * @param trackEnd absolute reader position at which the track ends.
   * @return all parsed events in their original track order.
   *
   * @throws IllegalStateException if running status is used before any
   * channel status has been established.
   * @throws IllegalArgumentException if an event exceeds the track boundary.
   */
  private fun readTrack(
    reader: Reader,
    trackEnd: Int
  ): List<Event> {
    val events = mutableListOf<Event>()

    // Stores the most recent channel-message status byte.
    //
    // Meta and SysEx events invalidate running status.
    var runningStatus: Int? = null

    while (reader.position < trackEnd) {

      // Every MIDI event starts with a delta-time.
      val deltaTime = reader.readVariableLengthValue()

      require(reader.position < trackEnd) {
        "Unexpected end of MIDI track after delta time"
      }

      // The first byte after the delta-time may either be:
      //
      // 1. a status byte (MSB = 1), or
      // 2. a data byte belonging to the previous running status (MSB = 0).
      val firstByte = reader.read1Byte()

      val status: Int
      val firstDataByte: Int?

      if (firstByte and 0x80 != 0) {
        // A byte with its MSB set is a status byte.
        status = firstByte
        firstDataByte = null
      } else {
        // A byte with its MSB clear is interpreted as the first data byte
        // of the previously active channel status.
        status = runningStatus
          ?: error(
            "Running status encountered without a previous channel status"
          )

        firstDataByte = firstByte
      }

      when {

        // ---------------------------------------------------------
        // META EVENTS
        // ---------------------------------------------------------

        status == 0xFF -> {
          val event = readMetaEvent(reader, deltaTime)

          events += event

          // Meta events terminate running status.
          runningStatus = null

          // End of Track marks the logical end of this track.
          if (event is EndOfTrackMetaEvent) {
            break
          }
        }

        // ---------------------------------------------------------
        // SYSTEM EXCLUSIVE
        // ---------------------------------------------------------

        status == 0xF0 || status == 0xF7 -> {
          val event = readSysExEvent(
            reader,
            deltaTime,
            status
          )

          events += event

          // SysEx events terminate channel running status.
          runningStatus = null
        }

        // ---------------------------------------------------------
        // CHANNEL EVENTS
        // ---------------------------------------------------------

        status in 0x80..0xEF -> {
          val event = readChannelEvent(
            reader,
            deltaTime,
            status,
            firstDataByte
          )

          events += event

          // Channel events establish/update running status.
          runningStatus = status
        }

        else -> {
          error(
            "Invalid MIDI status byte: 0x${status.toString(16)}"
          )
        }
      }

      // An event is never allowed to consume bytes belonging to the next
      // track chunk.
      require(reader.position <= trackEnd) {
        "MIDI event exceeded track boundary"
      }
    }

    // Some files may contain padding after the logical End of Track event.
    // Consume any remaining bytes belonging to this track.
    if (reader.position < trackEnd) {
      reader.advancePosition(trackEnd - reader.position)
    }

    require(reader.position == trackEnd) {
      "Reader position does not match track end"
    }

    return events
  }

  // ---------------------------------------------------------
  // CHANNEL EVENTS
  // ---------------------------------------------------------

  /**
   * Parses a MIDI channel voice event.
   *
   * Channel voice status bytes use the following structure:
   *
   * ```
   * 1xxxcccc
   * ```
   *
   * where:
   *
   * - the upper nibble identifies the event type;
   * - the lower nibble identifies the MIDI channel.
   *
   * For example:
   *
   * ```
   * 0x90
   * ```
   *
   * means Note On, channel 0.
   *
   * The [firstDataByte] parameter is used when the event was encoded using
   * running status. In that case, the first byte read after the delta-time
   * was already a data byte and must not be read again.
   *
   * @param reader source binary reader.
   * @param deltaTime delta-time associated with the event.
   * @param status MIDI channel status byte.
   * @param firstDataByte first data byte when running status was used,
   * otherwise `null`.
   * @return the decoded channel event.
   */
  private fun readChannelEvent(
    reader: Reader,
    deltaTime: Int,
    status: Int,
    firstDataByte: Int?
  ): Event {
    // The upper four bits identify the channel-message type.
    val type = status and 0xF0

    // The lower four bits identify the MIDI channel.
    val channel = status and 0x0F

    return when (type) {

      // ---------------------------------------------------------
      // NOTE OFF
      // ---------------------------------------------------------

      0x80 -> {
        val note = firstDataByte ?: reader.read1Byte()
        val velocity = reader.read1Byte()

        NoteOffControlEvent(
          deltaTime,
          channel,
          note,
          velocity
        )
      }

      // ---------------------------------------------------------
      // NOTE ON
      // ---------------------------------------------------------

      0x90 -> {
        val note = firstDataByte ?: reader.read1Byte()
        val velocity = reader.read1Byte()

        NoteOnControlEvent(
          deltaTime,
          channel,
          note,
          velocity
        )
      }

      // ---------------------------------------------------------
      // POLYPHONIC KEY PRESSURE
      // ---------------------------------------------------------

      0xA0 -> {
        val note = firstDataByte ?: reader.read1Byte()
        val pressure = reader.read1Byte()

        PolyphonicKeyPressureControlEvent(
          deltaTime,
          channel,
          note,
          pressure
        )
      }

      // ---------------------------------------------------------
      // CONTROL CHANGE
      // ---------------------------------------------------------

      0xB0 -> {
        val controller = firstDataByte ?: reader.read1Byte()
        val value = reader.read1Byte()

        ControlChangeControlEvent(
          deltaTime,
          channel,
          controller,
          value
        )
      }

      // ---------------------------------------------------------
      // PROGRAM CHANGE
      // ---------------------------------------------------------

      0xC0 -> {
        val program = firstDataByte ?: reader.read1Byte()

        ProgramChangeControlEvent(
          deltaTime,
          channel,
          program
        )
      }

      // ---------------------------------------------------------
      // CHANNEL PRESSURE
      // ---------------------------------------------------------

      0xD0 -> {
        val pressure = firstDataByte ?: reader.read1Byte()

        ChannelPressureControlEvent(
          deltaTime,
          channel,
          pressure
        )
      }

      // ---------------------------------------------------------
      // PITCH BEND
      // ---------------------------------------------------------

      0xE0 -> {
        // MIDI Pitch Bend stores the 14-bit value as:
        //
        // byte 1 -> least significant 7 bits
        // byte 2 -> most significant 7 bits
        //
        // Therefore:
        //
        // bend = (MSB << 7) | LSB
        val lsb = firstDataByte ?: reader.read1Byte()
        val msb = reader.read1Byte()

        val bend = (msb shl 7) or lsb

        PitchBendControlEvent(
          deltaTime,
          channel,
          bend
        )
      }

      else -> {
        error(
          "Unknown MIDI channel event: 0x${type.toString(16)}"
        )
      }
    }
  }

  // ---------------------------------------------------------
  // META EVENTS
  // ---------------------------------------------------------

  /**
   * Parses a MIDI meta event.
   *
   * Meta events use the following structure:
   *
   * ```
   * FF type length data...
   * ```
   *
   * The `FF` marker is consumed by [readTrack]. This method therefore starts
   * by reading the event [type], followed by its variable-length payload size.
   *
   * Known standard meta-event types are converted into their corresponding
   * strongly typed [Event] classes.
   *
   * Unknown meta-event types are preserved as [UnknownMetaEvent] rather than
   * discarded. This allows the library to retain information from MIDI files
   * containing extensions or event types that are not explicitly modeled.
   *
   * @param reader source binary reader positioned immediately after `FF`.
   * @param deltaTime delta-time associated with the event.
   * @return the decoded meta event.
   */
  private fun readMetaEvent(
    reader: Reader,
    deltaTime: Int
  ): Event {
    val type = reader.read1Byte()
    val length = reader.readVariableLengthValue()

    require(length >= 0) {
      "Invalid MIDI meta event length: $length"
    }

    return when (type) {

      // ---------------------------------------------------------
      // SEQUENCE NUMBER
      // ---------------------------------------------------------

      0x00 -> {
        require(length == 2) {
          "Sequence Number meta event must have length 2"
        }

        SequenceNumberMetaEvent(
          deltaTime,
          reader.read2Bytes()
        )
      }

      // ---------------------------------------------------------
      // TEXT
      // ---------------------------------------------------------

      0x01 -> {
        TextMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // COPYRIGHT
      // ---------------------------------------------------------

      0x02 -> {
        CopyrightNoticeMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // TRACK NAME
      // ---------------------------------------------------------

      0x03 -> {
        TrackNameMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // INSTRUMENT NAME
      // ---------------------------------------------------------

      0x04 -> {
        InstrumentNameMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // LYRIC
      // ---------------------------------------------------------

      0x05 -> {
        LyricMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // MARKER
      // ---------------------------------------------------------

      0x06 -> {
        MarkerMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // CUE POINT
      // ---------------------------------------------------------

      0x07 -> {
        CuePointMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // PROGRAM NAME
      // ---------------------------------------------------------

      0x08 -> {
        ProgramNameMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // DEVICE NAME
      // ---------------------------------------------------------

      0x09 -> {
        DeviceNameMetaEvent(
          deltaTime,
          reader.readString(length)
        )
      }

      // ---------------------------------------------------------
      // MIDI CHANNEL PREFIX
      // ---------------------------------------------------------

      0x20 -> {
        require(length == 1) {
          "MIDI Channel Prefix meta event must have length 1"
        }

        MidiChannelPrefixMetaEvent(
          deltaTime,
          reader.read1Byte()
        )
      }

      // ---------------------------------------------------------
      // END OF TRACK
      // ---------------------------------------------------------

      0x2F -> {
        require(length == 0) {
          "End Of Track meta event must have length 0"
        }

        EndOfTrackMetaEvent(deltaTime)
      }

      // ---------------------------------------------------------
      // SET TEMPO
      // ---------------------------------------------------------

      0x51 -> {
        require(length == 3) {
          "Set Tempo meta event must have length 3"
        }

        SetTempoMetaEvent(
          deltaTime,
          reader.read3Bytes()
        )
      }

      // ---------------------------------------------------------
      // SMPTE OFFSET
      // ---------------------------------------------------------

      0x54 -> {
        require(length == 5) {
          "SMPTE Offset meta event must have length 5"
        }

        SmpteOffsetMetaEvent(
          deltaTime,
          reader.read1Byte(),
          reader.read1Byte(),
          reader.read1Byte(),
          reader.read1Byte(),
          reader.read1Byte()
        )
      }

      // ---------------------------------------------------------
      // TIME SIGNATURE
      // ---------------------------------------------------------

      0x58 -> {
        require(length == 4) {
          "Time Signature meta event must have length 4"
        }

        val numerator = reader.read1Byte()

        // MIDI stores the denominator as an exponent of two.
        //
        // Example:
        //
        // stored value = 2
        // denominator  = 2^2 = 4
        //
        // Therefore a stored exponent of 2 represents a 4 in the
        // conventional time-signature denominator.
        val denomExp = reader.read1Byte()

        val clocks = reader.read1Byte()
        val notes = reader.read1Byte()

        TimeSignatureMetaEvent(
          deltaTime,
          numerator,
          (1 shl denomExp),
          clocks,
          notes
        )
      }

      // ---------------------------------------------------------
      // KEY SIGNATURE
      // ---------------------------------------------------------

      0x59 -> {
        require(length == 2) {
          "Key Signature meta event must have length 2"
        }

        // The key field is a signed 8-bit value.
        //
        // MIDI uses negative values for flats and positive values
        // for sharps, so the raw unsigned byte must be converted
        // back into a signed value.
        val key = reader.read1Byte().toByte().toInt()

        val scale = reader.read1Byte()

        KeySignatureMetaEvent(
          deltaTime,
          key,
          scale
        )
      }

      // ---------------------------------------------------------
      // SEQUENCER SPECIFIC
      // ---------------------------------------------------------

      0x7F -> {
        SequencerSpecificMetaEvent(
          deltaTime,
          reader.readBytes(length).toByteArray()
        )
      }

      // ---------------------------------------------------------
      // UNKNOWN META EVENT
      // ---------------------------------------------------------

      else -> {
        // Preserve unsupported meta events instead of discarding them.
        // The original type and payload can therefore survive a
        // read/write round trip.
        UnknownMetaEvent(
          deltaTime,
          type,
          reader.readBytes(length).toByteArray()
        )
      }
    }
  }

  // ---------------------------------------------------------
  // SYSTEM EXCLUSIVE
  // ---------------------------------------------------------

  /**
   * Parses a MIDI System Exclusive event.
   *
   * Both standard SysEx events (`F0`) and SysEx escape events (`F7`) are
   * supported.
   *
   * The event structure is:
   *
   * ```
   * F0/F7
   * length
   * raw data...
   * ```
   *
   * where [length] is encoded as a MIDI Variable-Length Quantity.
   *
   * The distinction between `F0` and `F7` is preserved through the
   * [SysExEvent.escaped] property.
   *
   * @param reader source binary reader positioned immediately after the
   * `F0` or `F7` status byte.
   * @param deltaTime delta-time associated with the event.
   * @param status original SysEx status byte.
   * @return the decoded [SysExEvent].
   */
  private fun readSysExEvent(
    reader: Reader,
    deltaTime: Int,
    status: Int
  ): SysExEvent {
    // SysEx payload length is encoded as a MIDI VLQ.
    val length = reader.readVariableLengthValue()

    return SysExEvent(
      deltaTime = deltaTime,
      rawData = reader.readBytes(length).toByteArray(),

      // F7 identifies the escaped SysEx form.
      escaped = (status == 0xF7)
    )
  }
}