// MidiWriter.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Writer

/**
 * Serializes [Midi] objects into Standard MIDI File (SMF) binary data.
 *
 * This object is responsible for converting the library's high-level MIDI
 * model into the binary representation defined by the Standard MIDI File
 * specification.
 *
 * The writer handles:
 *
 * - SMF headers (`MThd`);
 * - MIDI track chunks (`MTrk`);
 * - variable-length delta-times;
 * - MIDI channel events;
 * - MIDI meta events;
 * - System Exclusive events;
 * - optional running status;
 * - automatic End of Track insertion.
 *
 * The writer does not modify the supplied [Midi] object. It creates a new
 * binary representation from the events contained in it.
 */
object MidiWriter {

  /**
   * Serializes a [Midi] object into a complete Standard MIDI File.
   *
   * The resulting byte array contains the SMF header followed by all track
   * chunks. Each track is encoded independently and its binary size is
   * calculated automatically.
   *
   * ### Output structure
   *
   * The generated file has the following general structure:
   *
   * ```
   * MThd
   * ├── header length
   * ├── format
   * ├── number of tracks
   * └── division
   *
   * MTrk
   * └── encoded track events
   *
   * MTrk
   * └── encoded track events
   *
   * ...
   * ```
   *
   * ### Running status
   *
   * By default, every channel event writes its status byte explicitly.
   * When [useRunningStatus] is `true`, consecutive channel events using the
   * same status byte may omit the status byte according to the MIDI running
   * status mechanism.
   *
   * For example, without running status:
   *
   * ```
   * 90 3C 64
   * 90 40 64
   * 90 43 64
   * ```
   *
   * With running status:
   *
   * ```
   * 90 3C 64
   * 40 64
   * 43 64
   * ```
   *
   * Meta events and System Exclusive events reset running status because they
   * are not channel voice messages.
   *
   * @param midi the decoded MIDI structure to serialize.
   * @param useRunningStatus whether channel events should use MIDI running
   * status when possible. Defaults to `false`.
   *
   * @return a [ByteArray] containing the complete SMF binary representation.
   *
   * @throws IllegalArgumentException if the MIDI format is outside `0..2`,
   * if there are no tracks, if there are more than `65535` tracks, or if the
   * division is outside the 16-bit MIDI header range.
   */
  fun toBytes(
    midi: Midi,
    useRunningStatus: Boolean = false
  ): ByteArray {
    require(midi.format in 0..2) {
      "Invalid MIDI format: ${midi.format}"
    }

    require(midi.tracks.isNotEmpty()) {
      "MIDI must contain at least one track"
    }

    require(midi.tracks.size <= 0xFFFF) {
      "Too many MIDI tracks: ${midi.tracks.size}"
    }

    // Format 0 normally contains exactly one track.
    // This is intentionally not enforced here so that validation policy
    // can remain separate from the low-level serialization logic.

    require(midi.division in 0..0xFFFF) {
      "Invalid MIDI division: ${midi.division}"
    }

    val writer = Writer()

    // ---------------------------------------------------------
    // HEADER CHUNK
    // ---------------------------------------------------------

    // "MThd" identifies the SMF header chunk.
    writer.writeString("MThd")

    // The SMF header data always occupies exactly six bytes:
    //
    // 2 bytes -> format
    // 2 bytes -> number of tracks
    // 2 bytes -> division
    writer.write4Bytes(6)

    writer.write2Bytes(midi.format)
    writer.write2Bytes(midi.tracks.size)
    writer.write2Bytes(midi.division)

    // ---------------------------------------------------------
    // TRACK CHUNKS
    // ---------------------------------------------------------

    midi.tracks.forEach { track ->
      val trackBody = writeTrack(track, useRunningStatus)

      // Each track is preceded by the "MTrk" identifier and a
      // four-byte length describing the encoded track body.
      writer.writeBytes(
        Writer.buildChunk(id = "MTrk", body = trackBody)
      )
    }

    return writer.toByteArray()
  }

  /**
   * Serializes a [Midi] object and writes the resulting bytes directly to
   * a file.
   *
   * This is a convenience method around [toBytes]. The MIDI structure is
   * first serialized completely in memory and then written using [Writer].
   *
   * ### Example
   *
   * ```
   * MidiWriter.writeToFile(
   *   midi = midi,
   *   path = "output.mid"
   * )
   * ```
   *
   * Running status can also be enabled:
   *
   * ```
   * MidiWriter.writeToFile(
   *   midi = midi,
   *   path = "output.mid",
   *   useRunningStatus = true
   * )
   * ```
   *
   * @param midi the MIDI structure to serialize.
   * @param path destination path for the generated MIDI file.
   * @param useRunningStatus whether to use MIDI running status when possible.
   */
  fun writeToFile(
    midi: Midi,
    path: String,
    useRunningStatus: Boolean = false
  ) {
    val bytes = toBytes(midi, useRunningStatus)

    Writer().apply {
      writeBytes(bytes.toUByteArray())
      saveToFile(path)
    }
  }

  /**
   * Serializes all events belonging to a single MIDI track.
   *
   * This method is responsible for encoding event delta-times and event
   * payloads. The returned data represents only the track body; the `MTrk`
   * identifier and four-byte track length are added by [toBytes].
   *
   * Each event is written in this order:
   *
   * ```
   * delta-time
   * event data
   * ```
   *
   * Delta-times are encoded as MIDI Variable-Length Quantities (VLQs).
   *
   * The method also maintains the running-status state for channel events.
   * Meta and SysEx events explicitly clear that state.
   *
   * If the supplied event list does not contain an [EndOfTrackMetaEvent],
   * a standard End of Track event with zero delta-time is appended
   * automatically.
   *
   * @param events the events belonging to the track, in their desired order.
   * @param useRunningStatus whether consecutive channel events may omit a
   * repeated status byte.
   *
   * @return the serialized track body without the `MTrk` header and length.
   *
   * @throws IllegalArgumentException if an event contains a negative
   * delta-time or an invalid event-specific value.
   */
  private fun writeTrack(
    events: List<Event>,
    useRunningStatus: Boolean
  ): UByteArray {
    val writer = Writer()

    // The status byte of the most recent channel event.
    //
    // Running status allows this byte to be omitted when the next channel
    // event uses exactly the same status byte.
    var runningStatus: Int? = null

    events.forEach { event ->

      require(event.deltaTime >= 0) {
        "Invalid delta time: ${event.deltaTime}"
      }

      // Delta-times are encoded as MIDI VLQs.
      writer.writeBytes(
        writer.writeVariableLengthQuantity(event.deltaTime).toUByteArray()
      )

      when (event) {

        // ---------------------------------------------------------
        // CHANNEL VOICE EVENTS
        // ---------------------------------------------------------

        is NoteOffControlEvent -> {
          // The upper nibble identifies Note Off (0x8).
          // The lower nibble identifies the MIDI channel.
          val status = 0x80 or (event.channel and 0x0F)

          writeChannelStatus(
            writer,
            status,
            runningStatus,
            useRunningStatus
          )

          writer.write1Byte(event.note)
          writer.write1Byte(event.velocity)

          // This status can be reused by the next compatible channel event.
          runningStatus = status
        }

        is NoteOnControlEvent -> {
          // The upper nibble identifies Note On (0x9).
          val status = 0x90 or (event.channel and 0x0F)

          writeChannelStatus(
            writer,
            status,
            runningStatus,
            useRunningStatus
          )

          writer.write1Byte(event.note)
          writer.write1Byte(event.velocity)

          runningStatus = status
        }

        is PolyphonicKeyPressureControlEvent -> {
          // The upper nibble 0xA identifies Polyphonic Key Pressure.
          val status = 0xA0 or (event.channel and 0x0F)

          writeChannelStatus(
            writer,
            status,
            runningStatus,
            useRunningStatus
          )

          writer.write1Byte(event.note)
          writer.write1Byte(event.pressure)

          runningStatus = status
        }

        is ControlChangeControlEvent -> {
          // The upper nibble 0xB identifies Control Change.
          val status = 0xB0 or (event.channel and 0x0F)

          writeChannelStatus(
            writer,
            status,
            runningStatus,
            useRunningStatus
          )

          writer.write1Byte(event.controller)
          writer.write1Byte(event.value)

          runningStatus = status
        }

        is ProgramChangeControlEvent -> {
          // Program Change uses status nibble 0xC and only one data byte.
          val status = 0xC0 or (event.channel and 0x0F)

          writeChannelStatus(
            writer,
            status,
            runningStatus,
            useRunningStatus
          )

          writer.write1Byte(event.program)

          runningStatus = status
        }

        is ChannelPressureControlEvent -> {
          // The upper nibble 0xD identifies Channel Pressure.
          val status = 0xD0 or (event.channel and 0x0F)

          writeChannelStatus(
            writer,
            status,
            runningStatus,
            useRunningStatus
          )

          writer.write1Byte(event.pressure)

          runningStatus = status
        }

        is PitchBendControlEvent -> {
          // The upper nibble 0xE identifies Pitch Bend.
          val status = 0xE0 or (event.channel and 0x0F)

          writeChannelStatus(
            writer,
            status,
            runningStatus,
            useRunningStatus
          )

          // Pitch Bend is a 14-bit value transmitted as two 7-bit bytes:
          //
          // first byte  -> least significant 7 bits
          // second byte -> most significant 7 bits
          //
          // The MIDI representation is therefore LSB first.
          writer.write1Byte(event.bend and 0x7F)
          writer.write1Byte((event.bend shr 7) and 0x7F)

          runningStatus = status
        }

        // ---------------------------------------------------------
        // META EVENTS
        // ---------------------------------------------------------

        // All meta events begin with 0xFF and therefore do not participate
        // in MIDI channel-message running status.
        is SequenceNumberMetaEvent -> {
          writeMetaHeader(writer, 0x00, 2)
          writer.write2Bytes(event.sequenceNumber)

          // Meta events terminate the current running-status context.
          runningStatus = null
        }

        is TextMetaEvent -> {
          writeMetaBytes(writer, 0x01, event.text)
          runningStatus = null
        }

        is CopyrightNoticeMetaEvent -> {
          writeMetaBytes(writer, 0x02, event.copyrightNotice)
          runningStatus = null
        }

        is TrackNameMetaEvent -> {
          writeMetaBytes(writer, 0x03, event.trackName)
          runningStatus = null
        }

        is InstrumentNameMetaEvent -> {
          writeMetaBytes(writer, 0x04, event.instrumentName)
          runningStatus = null
        }

        is LyricMetaEvent -> {
          writeMetaBytes(writer, 0x05, event.lyric)
          runningStatus = null
        }

        is MarkerMetaEvent -> {
          writeMetaBytes(writer, 0x06, event.marker)
          runningStatus = null
        }

        is CuePointMetaEvent -> {
          writeMetaBytes(writer, 0x07, event.cuePoint)
          runningStatus = null
        }

        is ProgramNameMetaEvent -> {
          writeMetaBytes(writer, 0x08, event.programName)
          runningStatus = null
        }

        is DeviceNameMetaEvent -> {
          writeMetaBytes(writer, 0x09, event.deviceName)
          runningStatus = null
        }

        is MidiChannelPrefixMetaEvent -> {
          writeMetaHeader(writer, 0x20, 1)
          writer.write1Byte(event.midiChannelPrefix)

          runningStatus = null
        }

        is EndOfTrackMetaEvent -> {
          // End of Track has an empty payload.
          writeMetaHeader(writer, 0x2F, 0)

          runningStatus = null
        }

        is SetTempoMetaEvent -> {
          // Set Tempo stores the duration of a quarter note in
          // microseconds using exactly three bytes.
          writeMetaHeader(writer, 0x51, 3)
          writer.write3Bytes(event.tempo)

          runningStatus = null
        }

        is SmpteOffsetMetaEvent -> {
          writeMetaHeader(writer, 0x54, 5)

          writer.write1Byte(event.hour)
          writer.write1Byte(event.minute)
          writer.write1Byte(event.second)
          writer.write1Byte(event.frame)
          writer.write1Byte(event.subframe)

          runningStatus = null
        }

        is TimeSignatureMetaEvent -> {
          // The SMF representation stores the denominator as an exponent
          // of two rather than storing the denominator itself.
          //
          // For example:
          //
          // denominator = 4
          // exponent   = 2
          //
          // because 2^2 = 4.
          require(
            event.denominator > 0 &&
                event.denominator and (event.denominator - 1) == 0
          ) {
            "Time signature denominator must be a power of two: ${event.denominator}"
          }

          val exp = Integer.numberOfTrailingZeros(event.denominator)

          writeMetaHeader(writer, 0x58, 4)
          writer.write1Byte(event.numerator)
          writer.write1Byte(exp)
          writer.write1Byte(event.clocksPerTick)
          writer.write1Byte(event.notesPer24Clocks)

          runningStatus = null
        }

        is KeySignatureMetaEvent -> {
          writeMetaHeader(writer, 0x59, 2)

          // The key is represented as a signed 8-bit value.
          // Masking with 0xFF preserves its raw byte representation.
          writer.write1Byte(event.key and 0xFF)
          writer.write1Byte(event.scale)

          runningStatus = null
        }

        is SequencerSpecificMetaEvent -> {
          // Sequencer-specific data has an arbitrary payload length.
          writeMetaHeader(writer, 0x7F, event.rawData.size)
          writer.writeBytes(event.rawData.toUByteArray())

          runningStatus = null
        }

        is UnknownMetaEvent -> {
          // Unknown meta events are preserved rather than discarded.
          // This allows the writer to round-trip unsupported meta-event
          // types as long as their original payload is available.
          require(event.type in 0..0xFF) {
            "Invalid meta event type: ${event.type}"
          }

          writeMetaHeader(
            writer,
            event.type,
            event.rawData.size
          )

          writer.writeBytes(event.rawData.toUByteArray())

          runningStatus = null
        }

        // ---------------------------------------------------------
        // SYSTEM EXCLUSIVE EVENTS
        // ---------------------------------------------------------

        is SysExEvent -> {
          // F0 starts a normal System Exclusive event.
          // F7 is used for a SysEx escape event.
          writer.write1Byte(
            if (event.escaped) 0xF7 else 0xF0
          )

          // SysEx payloads use a VLQ to describe their length.
          writer.writeBytes(
            writer.writeVariableLengthQuantity(event.rawData.size)
              .toUByteArray()
          )

          writer.writeBytes(event.rawData.toUByteArray())

          // SysEx events do not participate in channel-message running status.
          runningStatus = null
        }

        else -> {
          // Every supported Event subtype should be handled explicitly above.
          // Reaching this branch indicates that the model contains an event
          // type that this writer does not know how to serialize.
          error("Unsupported MIDI event: ${event::class.simpleName}")
        }
      }
    }

    // ---------------------------------------------------------
    // AUTOMATIC END OF TRACK
    // ---------------------------------------------------------

    // An SMF track must be terminated by an End of Track meta event.
    //
    // If the caller did not explicitly provide one, append:
    //
    // 00 FF 2F 00
    //
    // This corresponds to:
    //
    // 00 -> delta-time = 0
    // FF -> meta event marker
    // 2F -> End of Track type
    // 00 -> zero-length payload
    if (events.none { it is EndOfTrackMetaEvent }) {
      writer.writeBytes(
        writer.writeVariableLengthQuantity(0).toUByteArray()
      )
      writer.write1Byte(0xFF)
      writer.write1Byte(0x2F)
      writer.writeBytes(
        writer.writeVariableLengthQuantity(0).toUByteArray()
      )
    }

    return writer.toUByteArray()
  }

  /**
   * Writes a MIDI channel status byte when necessary.
   *
   * When running status is disabled, the status byte is always written.
   *
   * When running status is enabled, the status byte is omitted if it is
   * identical to the currently active [runningStatus].
   *
   * For example, when the current status is `0x90`:
   *
   * ```
   * status = 0x90
   * runningStatus = 0x90
   * useRunningStatus = true
   * ```
   *
   * nothing is written because the receiver can reuse the previous `0x90`.
   *
   * If the status changes, the new status byte must be written.
   *
   * @param writer destination binary writer.
   * @param status channel status byte that would normally be written.
   * @param runningStatus status byte currently active for running status.
   * @param useRunningStatus whether running status compression is enabled.
   */
  private fun writeChannelStatus(
    writer: Writer,
    status: Int,
    runningStatus: Int?,
    useRunningStatus: Boolean
  ) {
    if (!useRunningStatus || runningStatus != status) {
      writer.write1Byte(status)
    }
  }

  /**
   * Writes the common header used by MIDI meta events.
   *
   * A meta event has the following binary structure:
   *
   * ```
   * FF type length data...
   * ```
   *
   * where:
   *
   * - `FF` identifies the event as a meta event;
   * - [type] identifies the specific meta-event type;
   * - [length] is encoded as a MIDI VLQ;
   * - the actual payload follows afterward.
   *
   * This helper writes only the header. The caller is responsible for writing
   * the event payload afterward.
   *
   * @param writer destination binary writer.
   * @param type meta-event type byte.
   * @param length number of payload bytes that follow the header.
   */
  private fun writeMetaHeader(
    writer: Writer,
    type: Int,
    length: Int
  ) {
    // FF identifies a MIDI meta event.
    writer.write1Byte(0xFF)

    // The second byte identifies the specific meta-event type.
    writer.write1Byte(type)

    // Meta-event lengths are MIDI VLQs.
    writer.writeBytes(
      writer.writeVariableLengthQuantity(length).toUByteArray()
    )
  }

  /**
   * Writes a text-based MIDI meta event.
   *
   * This helper is used by metadata events whose payload consists of text,
   * such as Track Name, Lyric, Marker, and Copyright Notice.
   *
   * The encoded structure is:
   *
   * ```
   * FF type length text
   * ```
   *
   * The [length] field describes the number of bytes in the encoded text.
   *
   * @param writer destination binary writer.
   * @param type meta-event type byte.
   * @param data textual payload to write.
   */
  private fun writeMetaBytes(
    writer: Writer,
    type: Int,
    data: String
  ) {
    writer.write1Byte(0xFF)
    writer.write1Byte(type)

    // The MIDI event length represents the number of bytes in the payload.
    writer.writeBytes(
      writer.writeVariableLengthQuantity(data.length).toUByteArray()
    )

    writer.writeString(data)
  }
}