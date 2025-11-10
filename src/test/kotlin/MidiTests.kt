@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

import com.lucasalfare.flmidi.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

  private lateinit var midi0: Midi
  private lateinit var midi1: Midi

  @BeforeEach
  fun setUp() {
    midi0 = readMidiFromBytes(format0Midi)
    midi1 = readMidiFromBytes(format1Midi)
  }

  // --- Helpers ---

  /**
   * Asserts that the event is of the expected type and returns it casted.
   */
  private inline fun <reified T> assertIsAndReturn(event: Event): T {
    assertTrue(event is T, "Expected event of type ${T::class.simpleName} but was ${event::class.simpleName}")
    @Suppress("UNCHECKED_CAST")
    return event as T
  }

  // --- Basic header and track tests ---

  @Test
  fun `header signature is MThd`() {
    assertEquals("MThd", midi0.header.signature)
    assertEquals("MThd", midi1.header.signature)
  }

  @Test
  fun `header length is 6 bytes`() {
    assertEquals(6, midi0.header.length)
    assertEquals(6, midi1.header.length)
  }

  @Test
  fun `header formats`() {
    assertEquals(0, midi0.header.format)
    assertEquals(1, midi1.header.format)
  }

  @Test
  fun `header number of tracks`() {
    assertEquals(1, midi0.header.numTracks)
    assertEquals(4, midi1.header.numTracks)
  }

  @Test
  fun `header division`() {
    assertEquals(96, midi0.header.division)
    assertEquals(96, midi1.header.division)
  }

  @Test
  fun `format0 has single track`() {
    assertNotNull(midi0.tracks.singleOrNull())
  }

  @Test
  fun `format1 tracks exist and count matches header`() {
    assertTrue(midi1.tracks.isNotEmpty())
    assertEquals(midi1.tracks.size, midi1.header.numTracks)
  }

  @Test
  fun `tracks signatures are MTrk`() {
    assertEquals("MTrk", midi0.tracks.first().signature)
    midi1.tracks.forEach { assertEquals("MTrk", it.signature) }
  }

  @Test
  fun `tracks lengths match declared values`() {
    assertEquals(59, midi0.tracks.first().length)

    val expectedLengths = intArrayOf(20, 16, 15, 21)
    midi1.tracks.forEachIndexed { index, track ->
      assertEquals(expectedLengths[index], track.length)
    }
  }

  // --- Format 0 event tests ---

  @Test
  fun `format0 events count`() {
    val events = midi0.tracks.first().events
    assertTrue(events.isNotEmpty())
    assertEquals(14, events.size)
  }

  @Test
  fun `format0 events content`() {
    val events = midi0.tracks.first().events

    var e = events[0]
    val t = assertIsAndReturn<TimeSignatureMetaEvent>(e)
    assertEquals(0, t.deltaTime)
    assertEquals(4, t.numerator)
    assertEquals(4, t.denominator)
    assertEquals(24, t.clocksPerTick)
    assertEquals(8, t.notesPer24Clocks)

    e = events[1]
    val tempo = assertIsAndReturn<SetTempoMetaEvent>(e)
    assertEquals(0, tempo.deltaTime)
    assertEquals(500000, tempo.tempo)

    e = events[2]
    val pc1 = assertIsAndReturn<ProgramChangeControlEvent>(e)
    assertEquals(0, pc1.deltaTime)
    assertEquals(0, pc1.channel)
    assertEquals(5, pc1.program)

    e = events[3]
    val pc2 = assertIsAndReturn<ProgramChangeControlEvent>(e)
    assertEquals(0, pc2.deltaTime)
    assertEquals(1, pc2.channel)
    assertEquals(46, pc2.program)

    e = events[4]
    val pc3 = assertIsAndReturn<ProgramChangeControlEvent>(e)
    assertEquals(0, pc3.deltaTime)
    assertEquals(2, pc3.channel)
    assertEquals(70, pc3.program)

    e = events[5]
    val n0 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(0, n0.deltaTime)
    assertEquals(2, n0.channel)
    assertEquals(48, n0.note)
    assertEquals(96, n0.velocity)

    e = events[6]
    val n1 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(0, n1.deltaTime)
    assertEquals(2, n1.channel)
    assertEquals(60, n1.note)
    assertEquals(96, n1.velocity)

    e = events[7]
    val n2 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(96, n2.deltaTime)
    assertEquals(1, n2.channel)
    assertEquals(67, n2.note)
    assertEquals(64, n2.velocity)

    e = events[8]
    val n3 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(96, n3.deltaTime)
    assertEquals(0, n3.channel)
    assertEquals(76, n3.note)
    assertEquals(32, n3.velocity)

    e = events[9]
    val off0 = assertIsAndReturn<NoteOffControlEvent>(e)
    assertEquals(192, off0.deltaTime)
    assertEquals(2, off0.channel)
    assertEquals(48, off0.note)
    assertEquals(64, off0.velocity)

    e = events[10]
    val off1 = assertIsAndReturn<NoteOffControlEvent>(e)
    assertEquals(0, off1.deltaTime)
    assertEquals(2, off1.channel)
    assertEquals(60, off1.note)
    assertEquals(64, off1.velocity)

    e = events[11]
    val off2 = assertIsAndReturn<NoteOffControlEvent>(e)
    assertEquals(0, off2.deltaTime)
    assertEquals(1, off2.channel)
    assertEquals(67, off2.note)
    assertEquals(64, off2.velocity)

    e = events[12]
    val off3 = assertIsAndReturn<NoteOffControlEvent>(e)
    assertEquals(0, off3.deltaTime)
    assertEquals(0, off3.channel)
    assertEquals(76, off3.note)
    assertEquals(64, off3.velocity)

    e = events[13]
    val end = assertIsAndReturn<EndOfTrackMetaEvent>(e)
    assertEquals(0, end.deltaTime)
  }

  // --- Format 1 event tests ---

  @Test
  fun `format1 track0 events`() {
    val tracks = midi1.tracks

    var e = tracks[0].events[0]
    val t = assertIsAndReturn<TimeSignatureMetaEvent>(e)
    assertEquals(0, t.deltaTime)
    assertEquals(4, t.numerator)
    assertEquals(4, t.denominator)
    assertEquals(24, t.clocksPerTick)
    assertEquals(8, t.notesPer24Clocks)

    e = tracks[0].events[1]
    val tempo = assertIsAndReturn<SetTempoMetaEvent>(e)
    assertEquals(0x07A120, tempo.tempo)

    e = tracks[0].events[2]
    val eot0 = assertIsAndReturn<EndOfTrackMetaEvent>(e)
    assertEquals(384, eot0.deltaTime)

    // track 1
    e = tracks[1].events[0]
    val pc = assertIsAndReturn<ProgramChangeControlEvent>(e)
    assertEquals(0, pc.deltaTime)
    assertEquals(0, pc.channel)
    assertEquals(5, pc.program)

    e = tracks[1].events[1]
    val noteA = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(192, noteA.deltaTime)
    assertEquals(0, noteA.channel)
    assertEquals(76, noteA.note)
    assertEquals(32, noteA.velocity)

    e = tracks[1].events[2]
    val noteAoff = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(192, noteAoff.deltaTime)
    assertEquals(0, noteAoff.channel)
    assertEquals(76, noteAoff.note)
    assertEquals(0, noteAoff.velocity)

    val eot1 = assertIsAndReturn<EndOfTrackMetaEvent>(tracks[1].events[3])
    assertEquals(0, eot1.deltaTime)

    // track 2
    e = tracks[2].events[0]
    val pc2 = assertIsAndReturn<ProgramChangeControlEvent>(e)
    assertEquals(0, pc2.deltaTime)
    assertEquals(1, pc2.channel)
    assertEquals(46, pc2.program)

    e = tracks[2].events[1]
    val noteG = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(96, noteG.deltaTime)
    assertEquals(1, noteG.channel)
    assertEquals(67, noteG.note)
    assertEquals(64, noteG.velocity)

    e = tracks[2].events[2]
    val noteGoff = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(288, noteGoff.deltaTime)
    assertEquals(1, noteGoff.channel)
    assertEquals(67, noteGoff.note)
    assertEquals(0, noteGoff.velocity)

    val eot2 = assertIsAndReturn<EndOfTrackMetaEvent>(tracks[2].events[3])
    assertEquals(0, eot2.deltaTime)

    // track 3
    e = tracks[3].events[0]
    val pc3 = assertIsAndReturn<ProgramChangeControlEvent>(e)
    assertEquals(0, pc3.deltaTime)
    assertEquals(2, pc3.channel)
    assertEquals(70, pc3.program)

    e = tracks[3].events[1]
    val n0 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(0, n0.deltaTime)
    assertEquals(2, n0.channel)
    assertEquals(48, n0.note)
    assertEquals(96, n0.velocity)

    e = tracks[3].events[2]
    val n1 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(0, n1.deltaTime)
    assertEquals(2, n1.channel)
    assertEquals(60, n1.note)
    assertEquals(96, n1.velocity)

    e = tracks[3].events[3]
    val n2 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(384, n2.deltaTime)
    assertEquals(2, n2.channel)
    assertEquals(48, n2.note)
    assertEquals(0, n2.velocity)

    e = tracks[3].events[4]
    val n3 = assertIsAndReturn<NoteOnControlEvent>(e)
    assertEquals(0, n3.deltaTime)
    assertEquals(2, n3.channel)
    assertEquals(60, n3.note)
    assertEquals(0, n3.velocity)

    val eot3 = assertIsAndReturn<EndOfTrackMetaEvent>(tracks[3].events[5])
    assertEquals(0, eot3.deltaTime)
  }
}