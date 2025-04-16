@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

import com.lucasalfare.flmidi.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ## MIDI Specification Compliance Tests
 *
 * This test class validates the correct parsing and interpretation of MIDI files
 * using official example byte streams from the standard MIDI file specification.
 *
 * Source of the examples: [Standard MIDI File Format Specification](https://www.music.mcgill.ca/~ich/classes/mumt306/StandardMIDIfileformat.html)
 *
 * ### Covered Formats
 * - **Format 0**: Single track with combined event stream.
 * - **Format 1**: Multiple synchronous tracks.
 *
 * The referenced page does **not** provide a Format 2 example, so Format 2 support is assumed
 * functional but is not tested here.
 *
 * ### Methodology
 * The examples provided by the specification are presented as raw hexadecimal bytes.
 * In this test class, those byte arrays are directly encoded using `ubyteArrayOf()` and then converted
 * to `ByteArray` to simulate actual file loading. This approach allows precise debugging and transparent
 * alignment with the official specification.
 *
 * Each test case targets a specific aspect of MIDI file structure:
 * - Header validation: signature, format type, track count, and time division.
 * - Track chunk validation: signatures, lengths, and events.
 * - Event-level validation for Format 0: includes meta events, control changes, and note events.
 *
 * ### Future Improvements
 * - Include Format 2 examples when available.
 * - Add more diverse event cases, covering currently missing MIDI event types such as:
 *   - System Exclusive Events (SysEx)
 *   - Advanced meta events
 *   - Pitch Bend, Aftertouch, etc.
 *
 * These tests serve as the foundation for ensuring spec-compliant MIDI parsing behavior in this library.
 */
class MidiTests {

  // raw bytes from official example MIDI file of format 0
  private val format0Midi = ubyteArrayOf(
    0x4Du, 0x54u, 0x68u, 0x64u,
    0x00u, 0x00u, 0x00u, 0x06u,
    0x00u, 0x00u,
    0x00u, 0x01u,
    0x00u, 0x60u,

    0x4Du, 0x54u, 0x72u, 0x6Bu,
    0x00u, 0x00u, 0x00u, 0x3Bu,
    0x00u, 0xFFu, 0x58u, 0x04u, 0x04u, 0x02u, 0x18u, 0x08u,
    0x00u, 0xFFu, 0x51u, 0x03u, 0x07u, 0xA1u, 0x20u,
    0x00u, 0xC0u, 0x05u,
    0x00u, 0xC1u, 0x2Eu,
    0x00u, 0xC2u, 0x46u,
    0x00u, 0x92u, 0x30u, 0x60u,
    0x00u, 0x3Cu, 0x60u,
    0x60u, 0x91u, 0x43u, 0x40u,
    0x60u, 0x90u, 0x4Cu, 0x20u,
    0x81u, 0x40u, 0x82u, 0x30u, 0x40u,
    0x00u, 0x3Cu, 0x40u,
    0x00u, 0x81u, 0x43u, 0x40u,
    0x00u, 0x80u, 0x4Cu, 0x40u,
    0x00u, 0xFFu, 0x2Fu, 0x00u
  ).toByteArray()

  // raw bytes from official example MIDI file of format 1
  private val format1Midi = ubyteArrayOf(
    0x4Du, 0x54u, 0x68u, 0x64u,
    0x00u, 0x00u, 0x00u, 0x06u,
    0x00u, 0x01u,
    0x00u, 0x04u,
    0x00u, 0x60u,

    0x4Du, 0x54u, 0x72u, 0x6Bu,
    0x00u, 0x00u, 0x00u, 0x14u,

    0x00u, 0xFFu, 0x58u, 0x04u, 0x04u, 0x02u, 0x18u, 0x08u,
    0x00u, 0xFFu, 0x51u, 0x03u, 0x07u, 0xA1u, 0x20u,
    0x83u, 0x00u, 0xFFu, 0x2Fu, 0x00u,

    0x4Du, 0x54u, 0x72u, 0x6Bu,
    0x00u, 0x00u, 0x00u, 0x10u,
    0x00u, 0xC0u, 0x05u,
    0x81u, 0x40u, 0x90u, 0x4Cu, 0x20u,
    0x81u, 0x40u, 0x4Cu, 0x00u,
    0x00u, 0xFFu, 0x2Fu, 0x00u,

    0x4Du, 0x54u, 0x72u, 0x6Bu,
    0x00u, 0x00u, 0x00u, 0x0Fu,
    0x00u, 0xC1u, 0x2Eu,
    0x60u, 0x91u, 0x43u, 0x40u,
    0x82u, 0x20u, 0x43u, 0x00u,
    0x00u, 0xFFu, 0x2Fu, 0x00u,

    0x4Du, 0x54u, 0x72u, 0x6Bu,
    0x00u, 0x00u, 0x00u, 0x15u,
    0x00u, 0xC2u, 0x46u,
    0x00u, 0x92u, 0x30u, 0x60u,
    0x00u, 0x3Cu, 0x60u,
    0x83u, 0x00u, 0x30u, 0x00u,
    0x00u, 0x3Cu, 0x00u,
    0x00u, 0xFFu, 0x2Fu, 0x00u
  ).toByteArray()

  /**
   * Ensures the MIDI header signature matches "MThd" as per the specification.
   */
  @Test
  fun `test header signatures`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = "MThd", midi0.header.signature)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = "MThd", midi1.header.signature)
  }

  /**
   * Validates that the declared header length is 6 bytes, as mandated for all standard MIDI files.
   */
  @Test
  fun `test header lengths`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 6, midi0.header.length)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 6, midi1.header.length)
  }

  /**
   * Confirms that the format field in the header correctly identifies MIDI format 0 and 1.
   */
  @Test
  fun `test header formats`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 0, midi0.header.format)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 1, midi1.header.format)
  }

  /**
   * Verifies the number of tracks declared in the header corresponds to the actual track chunks parsed.
   */
  @Test
  fun `test header number of tracks`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 1, midi0.header.numTracks)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 4, midi1.header.numTracks)
  }

  /**
   * Checks that the division field (timebase resolution) is correctly parsed from the header.
   */
  @Test
  fun `test header time division resolutions`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 96, midi0.header.division)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 96, midi1.header.division)
  }

  /**
   * Asserts that format 0 MIDI files contain exactly one track chunk.
   */
  @Test
  fun `test midiFormat0 track existence`() {
    val midi = readMidiFromBytes(format0Midi)
    assertNotNull(midi.tracks.singleOrNull())
  }

  /**
   * Ensures all track chunks in a format 1 MIDI file exist and are consistent with the header's declared count.
   */
  @Test
  fun `test midiFormat1 tracks existence`() {
    val midi = readMidiFromBytes(format1Midi)
    assertTrue(midi.tracks.isNotEmpty())
    assertEquals(expected = midi.tracks.size, actual = midi.header.numTracks)
  }

  /**
   * Validates that each track chunk begins with the expected "MTrk" signature.
   */
  @Test
  fun `test MIDIs tracks signatures`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = "MTrk", actual = midi0.tracks.first().signature)

    val midi1 = readMidiFromBytes(format1Midi)
    midi1.tracks.forEach {
      assertEquals(expected = "MTrk", actual = it.signature)
    }
  }

  /**
   * Asserts that track lengths match the values declared in the byte stream.
   */
  @Test
  fun `test MIDIs tracks lengths`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 59, actual = midi0.tracks.first().length)

    val midi1 = readMidiFromBytes(format1Midi)
    val expectedLengths = intArrayOf(20, 16, 15, 21)
    midi1.tracks.forEachIndexed { index, track ->
      assertEquals(expected = expectedLengths[index], actual = track.length)
    }
  }

  // FORMAT 0 EVENTS TESTING

  /**
   * Confirms that all events in the format 0 file are parsed and counted correctly.
   */
  @Test
  fun `test MIDI0 track events count`() {
    val midi0 = readMidiFromBytes(format0Midi)
    val events = midi0.tracks.first().events
    assertTrue(events.isNotEmpty())
    assertTrue(events.size == 14)
  }

  /**
   * Tests each event in the format 0 MIDI file for expected type and correct parsed data.
   * Includes validation for Time Signature, Set Tempo, Program Change and Note events.
   */
  @Test
  fun `test MIDI0 track events`() {
    val midi0 = readMidiFromBytes(format0Midi)
    val events = midi0.tracks.first().events

    var e = events[0]
    assertTrue(e is TimeSignatureMetaEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 4, actual = e.numerator)
    assertEquals(expected = 4, actual = e.denominator)
    assertEquals(expected = 24, actual = e.clocksPerTick)
    assertEquals(expected = 8, actual = e.notesPer24Clocks)

    e = events[1]
    assertTrue(e is SetTempoMetaEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 500000, actual = e.tempo)

    e = events[2]
    assertTrue(e is ProgramChangeControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 1 - 1, actual = e.channel)
    assertEquals(expected = 5, actual = e.program)

    e = events[3]
    assertTrue(e is ProgramChangeControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 2 - 1, actual = e.channel)
    assertEquals(expected = 46, actual = e.program)

    e = events[4]
    assertTrue(e is ProgramChangeControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 3 - 1, actual = e.channel)
    assertEquals(expected = 70, actual = e.program)

    e = events[5]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 3 - 1, actual = e.channel)
    assertEquals(expected = 48, actual = e.note) // note C3
    assertEquals(expected = 96, actual = e.velocity) // velocity "forte"

    e = events[6]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 3 - 1, actual = e.channel)
    assertEquals(expected = 60, actual = e.note) // note C4
    assertEquals(expected = 96, actual = e.velocity) // velocity "forte"

    e = events[7]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 96, actual = e.deltaTime)
    assertEquals(expected = 2 - 1, actual = e.channel)
    assertEquals(expected = 67, actual = e.note) // note G4
    assertEquals(expected = 64, actual = e.velocity) // velocity "mezzo-forte"

    e = events[8]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 96, actual = e.deltaTime)
    assertEquals(expected = 1 - 1, actual = e.channel)
    assertEquals(expected = 76, actual = e.note) // note E5
    assertEquals(expected = 32, actual = e.velocity) // velocity "piano"

    e = events[9]
    assertTrue(e is NoteOffControlEvent)
    assertEquals(expected = 192, actual = e.deltaTime)
    assertEquals(expected = 3 - 1, actual = e.channel)
    assertEquals(expected = 48, actual = e.note) // note C3
    assertEquals(expected = 64, actual = e.velocity) // velocity "standard"

    e = events[10]
    assertTrue(e is NoteOffControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 3 - 1, actual = e.channel)
    assertEquals(expected = 60, actual = e.note) // note C4
    assertEquals(expected = 64, actual = e.velocity) // velocity "standard"

    e = events[11]
    assertTrue(e is NoteOffControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 2 - 1, actual = e.channel)
    assertEquals(expected = 67, actual = e.note) // note G4
    assertEquals(expected = 64, actual = e.velocity) // velocity "standard"

    e = events[12]
    assertTrue(e is NoteOffControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 1 - 1, actual = e.channel)
    assertEquals(expected = 76, actual = e.note) // note E5
    assertEquals(expected = 64, actual = e.velocity) // velocity "standard"

    e = events[13]
    assertTrue(e is EndOfTrackMetaEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
  }

  /**
   * Checks the type and correctness of each event in a format 0 MIDI track.
   *
   * Includes:
   * - Time Signature Meta Event
   * - Set Tempo Meta Event
   * - Program Change Event
   * - Note On and Note Off Events
   */
  @Test
  fun `test MIDI1 track0 events`() {
    val midi = readMidiFromBytes(format1Midi)
    val tracks = midi.tracks

    // track 0 events
    var e = tracks[0].events[0]
    assertTrue(e is TimeSignatureMetaEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 4, actual = e.numerator)
    assertEquals(expected = 4, actual = e.denominator)
    assertEquals(expected = 24, actual = e.clocksPerTick)
    assertEquals(expected = 8, actual = e.notesPer24Clocks)

    e = tracks[0].events[1]
    assertTrue(e is SetTempoMetaEvent)
    assertEquals(expected = 0x07A120, actual = e.tempo)

    e = tracks[0].events[2]
    assertTrue(e is EndOfTrackMetaEvent)
    assertEquals(expected = 384, actual = e.deltaTime)

    // track 1 events
    e = tracks[1].events[0]
    assertTrue(e is ProgramChangeControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 0, actual = e.channel)
    assertEquals(expected = 5, actual = e.program)

    e = tracks[1].events[1]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 192, actual = e.deltaTime)
    assertEquals(expected = 0, actual = e.channel)
    assertEquals(expected = 76, actual = e.note) // note E5
    assertEquals(expected = 32, actual = e.velocity)

    e = tracks[1].events[2]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 192, actual = e.deltaTime)
    assertEquals(expected = 0, actual = e.channel)
    assertEquals(expected = 76, actual = e.note)
    assertEquals(expected = 0, actual = e.velocity) // note on with velocity 0 means same as note off

    e = tracks[1].events[3]
    assertTrue(e is EndOfTrackMetaEvent)
    assertEquals(expected = 0, actual = e.deltaTime)

    // track 2 events
    e = tracks[2].events[0]
    assertTrue(e is ProgramChangeControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 1, actual = e.channel)
    assertEquals(expected = 46, actual = e.program)

    e = tracks[2].events[1]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 96, actual = e.deltaTime)
    assertEquals(expected = 1, actual = e.channel)
    assertEquals(expected = 67, actual = e.note) // note G4
    assertEquals(expected = 64, actual = e.velocity)

    e = tracks[2].events[2]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 288, actual = e.deltaTime)
    assertEquals(expected = 1, actual = e.channel)
    assertEquals(expected = 67, actual = e.note)
    assertEquals(expected = 0, actual = e.velocity) // note on with velocity 0 means same as note off

    e = tracks[2].events[3]
    assertTrue(e is EndOfTrackMetaEvent)
    assertEquals(expected = 0, actual = e.deltaTime)

    // track 3 events
    e = tracks[3].events[0]
    assertTrue(e is ProgramChangeControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 2, actual = e.channel)
    assertEquals(expected = 70, actual = e.program)

    e = tracks[3].events[1]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 2, actual = e.channel)
    assertEquals(expected = 48, actual = e.note) // note C3
    assertEquals(expected = 96, actual = e.velocity)

    e = tracks[3].events[2]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 2, actual = e.channel)
    assertEquals(expected = 60, actual = e.note) // note C4
    assertEquals(expected = 96, actual = e.velocity)

    e = tracks[3].events[3]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 384, actual = e.deltaTime)
    assertEquals(expected = 2, actual = e.channel)
    assertEquals(expected = 48, actual = e.note)
    assertEquals(expected = 0, actual = e.velocity) // note on with velocity 0 means same as note off

    e = tracks[3].events[4]
    assertTrue(e is NoteOnControlEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
    assertEquals(expected = 2, actual = e.channel)
    assertEquals(expected = 60, actual = e.note)
    assertEquals(expected = 0, actual = e.velocity) // note on with velocity 0 means same as note off

    e = tracks[3].events[5]
    assertTrue(e is EndOfTrackMetaEvent)
    assertEquals(expected = 0, actual = e.deltaTime)
  }
}