@file:OptIn(ExperimentalUnsignedTypes::class)

/**
 * Official Standard MIDI File examples used as binary fixtures in the test module.
 *
 * The byte arrays in this object represent complete MIDI files encoded directly
 * as raw binary data. They are intentionally kept as explicit byte sequences
 * instead of being generated through the library itself.
 *
 * Keeping these files as hard-coded binary fixtures is important for testing:
 *
 * - MIDI parsing against known byte sequences
 * - Header parsing
 * - Track chunk parsing
 * - MIDI file formats 0 and 1
 * - Variable-length quantities
 * - Running status
 * - Channel events
 * - Meta events
 * - Multiple-track files
 * - End-of-track events
 * - Round-trip serialization
 *
 * The fixtures should not depend on MidiWriter or any other FLMidi
 * implementation. Otherwise, a bug shared between the writer and reader
 * could cause tests to pass incorrectly.
 */
object OfficialMidiExamples {

  /**
   * Standard MIDI File Format 0 example.
   *
   * This file contains:
   *
   * - One MIDI header chunk (`MThd`)
   * - Format 0
   * - One track
   * - Division of 96 ticks per quarter note
   * - Time signature of 4/4
   * - Tempo of 500,000 microseconds per quarter note (120 BPM)
   * - Program changes on channels 0, 1, and 2
   * - Note events on multiple channels
   * - Running-status encoded channel events
   * - An End-of-Track meta event
   *
   * The byte sequence is stored exactly as it appears in the MIDI file,
   * including chunk identifiers, chunk lengths, event data, and all delta times.
   *
   * This fixture is useful for validating the reader's ability to parse a
   * complete Format 0 file and correctly interpret events encoded using
   * running status.
   */
  val format0 = ubyteArrayOf(
    // MThd
    0x4Du, 0x54u, 0x68u, 0x64u,
    // Header length: 6 bytes
    0x00u, 0x00u, 0x00u, 0x06u,
    // Format 0
    0x00u, 0x00u,
    // Number of tracks: 1
    0x00u, 0x01u,
    // Division: 96 ticks per quarter note
    0x00u, 0x60u,

    // MTrk
    0x4Du, 0x54u, 0x72u, 0x6Bu,
    // Track length: 59 bytes
    0x00u, 0x00u, 0x00u, 0x3Bu,

    // Delta time 0, Time Signature: 4/4
    0x00u, 0xFFu, 0x58u, 0x04u, 0x04u, 0x02u, 0x18u, 0x08u,

    // Delta time 0, Tempo: 500,000 microseconds per quarter note
    0x00u, 0xFFu, 0x51u, 0x03u, 0x07u, 0xA1u, 0x20u,

    // Delta time 0, Program Change, channel 0
    0x00u, 0xC0u, 0x05u,

    // Delta time 0, Program Change, channel 1
    0x00u, 0xC1u, 0x2Eu,

    // Delta time 0, Program Change, channel 2
    0x00u, 0xC2u, 0x46u,

    // Delta time 0, Note On, channel 2
    0x00u, 0x92u, 0x30u, 0x60u,

    // Delta time 0, Note On using running status (channel 2)
    0x00u, 0x3Cu, 0x60u,

    // Delta time 96, Note On, channel 1
    0x60u, 0x91u, 0x43u, 0x40u,

    // Delta time 96, Note On, channel 0
    0x60u, 0x90u, 0x4Cu, 0x20u,

    // Delta time 160, Note Off, channel 2
    0x81u, 0x40u, 0x82u, 0x30u, 0x40u,

    // Delta time 0, Note Off using running status (channel 2)
    0x00u, 0x3Cu, 0x40u,

    // Delta time 0, Note Off, channel 1
    0x00u, 0x81u, 0x43u, 0x40u,

    // Delta time 0, Note Off, channel 0
    0x00u, 0x80u, 0x4Cu, 0x40u,

    // Delta time 0, End of Track
    0x00u, 0xFFu, 0x2Fu, 0x00u
  ).toByteArray()

  /**
   * Standard MIDI File Format 1 example.
   *
   * This file contains:
   *
   * - One MIDI header chunk (`MThd`)
   * - Format 1
   * - Four independent track chunks
   * - Division of 96 ticks per quarter note
   * - Global timing information in the first track
   * - Instrument/program information and note events in subsequent tracks
   * - Running-status encoded channel events
   * - An End-of-Track meta event in every track
   *
   * Format 1 stores related musical information in multiple synchronous
   * tracks. Unlike Format 0, where all events are combined into a single
   * track, this fixture allows the parser to be tested against multiple
   * `MTrk` chunks belonging to the same MIDI file.
   *
   * The first track contains global timing information such as the time
   * signature and tempo. The remaining tracks contain channel-specific
   * musical events.
   */
  val format1 = ubyteArrayOf(
    // MThd
    0x4Du, 0x54u, 0x68u, 0x64u,
    // Header length: 6 bytes
    0x00u, 0x00u, 0x00u, 0x06u,
    // Format 1
    0x00u, 0x01u,
    // Number of tracks: 4
    0x00u, 0x04u,
    // Division: 96 ticks per quarter note
    0x00u, 0x60u,

    // -------------------------------------------------------------------------
    // Track 1: global timing information
    // -------------------------------------------------------------------------

    // MTrk
    0x4Du, 0x54u, 0x72u, 0x6Bu,
    // Track length: 20 bytes
    0x00u, 0x00u, 0x00u, 0x14u,

    // Delta time 0, Time Signature: 4/4
    0x00u, 0xFFu, 0x58u, 0x04u, 0x04u, 0x02u, 0x18u, 0x08u,

    // Delta time 0, Tempo: 500,000 microseconds per quarter note
    0x00u, 0xFFu, 0x51u, 0x03u, 0x07u, 0xA1u, 0x20u,

    // Delta time 896, End of Track
    0x83u, 0x00u, 0xFFu, 0x2Fu, 0x00u,

    // -------------------------------------------------------------------------
    // Track 2: channel 0
    // -------------------------------------------------------------------------

    // MTrk
    0x4Du, 0x54u, 0x72u, 0x6Bu,
    // Track length: 16 bytes
    0x00u, 0x00u, 0x00u, 0x10u,

    // Delta time 0, Program Change, channel 0
    0x00u, 0xC0u, 0x05u,

    // Delta time 192, Note On, channel 0
    0x81u, 0x40u, 0x90u, 0x4Cu, 0x20u,

    // Delta time 192, Note On using running status
    0x81u, 0x40u, 0x4Cu, 0x00u,

    // Delta time 0, End of Track
    0x00u, 0xFFu, 0x2Fu, 0x00u,

    // -------------------------------------------------------------------------
    // Track 3: channel 1
    // -------------------------------------------------------------------------

    // MTrk
    0x4Du, 0x54u, 0x72u, 0x6Bu,
    // Track length: 15 bytes
    0x00u, 0x00u, 0x00u, 0x0Fu,

    // Delta time 0, Program Change, channel 1
    0x00u, 0xC1u, 0x2Eu,

    // Delta time 96, Note On, channel 1
    0x60u, 0x91u, 0x43u, 0x40u,

    // Delta time 288, Note Off using running status
    0x82u, 0x20u, 0x43u, 0x00u,

    // Delta time 0, End of Track
    0x00u, 0xFFu, 0x2Fu, 0x00u,

    // -------------------------------------------------------------------------
    // Track 4: channel 2
    // -------------------------------------------------------------------------

    // MTrk
    0x4Du, 0x54u, 0x72u, 0x6Bu,
    // Track length: 21 bytes
    0x00u, 0x00u, 0x00u, 0x15u,

    // Delta time 0, Program Change, channel 2
    0x00u, 0xC2u, 0x46u,

    // Delta time 0, Note On, channel 2
    0x00u, 0x92u, 0x30u, 0x60u,

    // Delta time 0, Note On using running status
    0x00u, 0x3Cu, 0x60u,

    // Delta time 384, Note Off using running status
    0x83u, 0x00u, 0x30u, 0x00u,

    // Delta time 0, Note Off using running status
    0x00u, 0x3Cu, 0x00u,

    // Delta time 0, End of Track
    0x00u, 0xFFu, 0x2Fu, 0x00u
  ).toByteArray()
}