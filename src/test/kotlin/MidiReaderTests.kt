import com.lucasalfare.flmidi.EndOfTrackMetaEvent
import com.lucasalfare.flmidi.KeySignatureMetaEvent
import com.lucasalfare.flmidi.Midi
import com.lucasalfare.flmidi.MidiReader
import com.lucasalfare.flmidi.MidiWriter
import com.lucasalfare.flmidi.NoteOnControlEvent
import com.lucasalfare.flmidi.SequencerSpecificMetaEvent
import com.lucasalfare.flmidi.SysExEvent
import com.lucasalfare.flmidi.UnknownMetaEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for [MidiReader].
 *
 * These tests validate the reader against both predefined binary MIDI
 * fixtures and MIDI files generated programmatically through [MidiWriter].
 *
 * Two complementary testing strategies are used here:
 *
 * 1. Parse known binary MIDI files from [OfficialMidiExamples].
 *    This verifies that the reader can consume real MIDI byte sequences
 *    independently of the writer implementation.
 *
 * 2. Build MIDI models, serialize them with [MidiWriter], and read them
 *    back with [MidiReader].
 *    This verifies that the reader correctly interprets the binary
 *    representation produced by the library.
 *
 * The tests intentionally cover not only common channel events, but also
 * MIDI features such as running status, zero-velocity Note On events,
 * key signatures, SysEx events, unknown meta events, and sequencer-specific
 * meta events.
 */
class MidiReaderTests {

  /**
   * MIDI Format 0 fixture loaded from [OfficialMidiExamples.format0].
   */
  private lateinit var midi0: Midi

  /**
   * MIDI Format 1 fixture loaded from [OfficialMidiExamples.format1].
   */
  private lateinit var midi1: Midi

  /**
   * Loads the predefined MIDI fixtures before each test.
   *
   * The fixtures are parsed from their raw binary representation instead
   * of being constructed manually as [Midi] objects. This ensures that
   * the tests exercise the actual binary parsing logic of [MidiReader].
   */
  @BeforeEach
  fun setUp() {
    midi0 = MidiReader.fromBytes(OfficialMidiExamples.format0)
    midi1 = MidiReader.fromBytes(OfficialMidiExamples.format1)
  }

  /**
   * Verifies the basic structure of a Format 0 MIDI file.
   *
   * MIDI Format 0 stores all MIDI events in a single track. The fixture
   * used here therefore must be identified as Format 0 and contain exactly
   * one track.
   */
  @Test
  fun `format zero should have one track`() {
    assertEquals(0, midi0.format)
    assertEquals(1, midi0.tracks.size)
  }

  /**
   * Verifies the basic structure of a Format 1 MIDI file.
   *
   * MIDI Format 1 supports multiple synchronous tracks. The fixture used
   * here contains several tracks, so the reader must preserve them as
   * separate track collections.
   */
  @Test
  fun `format one should have multiple tracks`() {
    assertEquals(1, midi1.format)
    assertTrue(midi1.tracks.isNotEmpty())
  }

  /**
   * Verifies channel-event parsing and MIDI running status.
   *
   * The Format 0 fixture contains consecutive Note On events on channel 2.
   * The second event omits its status byte and relies on the running status
   * established by the first Note On event.
   *
   * The reader must therefore infer the missing status byte and produce
   * two independent [NoteOnControlEvent] instances with the correct
   * channel, note, and velocity values.
   */
  @Test
  fun `should read channel events and running status`() {
    val track = midi0.tracks.single()

    val firstNote = track[5] as NoteOnControlEvent
    val secondNote = track[6] as NoteOnControlEvent

    // Both events use channel 2 and velocity 96.
    // The second event is encoded using running status.
    assertEquals(2, firstNote.channel)
    assertEquals(48, firstNote.note)
    assertEquals(96, firstNote.velocity)

    assertEquals(2, secondNote.channel)
    assertEquals(60, secondNote.note)
    assertEquals(96, secondNote.velocity)
  }

  /**
   * Verifies that a Note On event with velocity zero is preserved as a
   * [NoteOnControlEvent].
   *
   * In the MIDI protocol, a Note On event whose velocity is zero is commonly
   * used as an alternative encoding for Note Off. However, the reader keeps
   * the event type represented by the actual status byte instead of silently
   * converting it into a [NoteOffControlEvent].
   *
   * The Format 1 fixture contains such an event for note 76.
   */
  @Test
  fun `should read note on event with zero velocity`() {
    val events = midi1.tracks[1]

    val event =
      events.find {
        it is NoteOnControlEvent && it.velocity == 0
      } as NoteOnControlEvent?

    assertNotNull(event)
    assertEquals(76, event!!.note)
    assertEquals(0, event.velocity)
  }

  /**
   * Verifies reading and writing of a Key Signature meta event.
   *
   * The test constructs a [KeySignatureMetaEvent], serializes it with
   * [MidiWriter], and then parses the resulting bytes with [MidiReader].
   *
   * The key value is negative, which is important because the MIDI
   * Key Signature meta event stores the number of sharps or flats as a
   * signed 8-bit value.
   *
   * The resulting event must be equal to the original event after the
   * write/read round trip.
   */
  @Test
  fun `should read key signature meta event`() {
    val keyEvent = KeySignatureMetaEvent(
      deltaTime = 0,
      key = (-3).toByte().toInt(),
      scale = 1
    )

    val track = listOf(
      keyEvent,
      EndOfTrackMetaEvent(0)
    )

    val midi = Midi(
      format = 0,
      division = 96,
      tracks = listOf(track)
    )

    val bytes = MidiWriter.toBytes(midi)
    val readBack = MidiReader.fromBytes(bytes)

    assertEquals(1, readBack.tracks.size)

    val readEvent =
      readBack.tracks[0][0] as KeySignatureMetaEvent

    assertEquals(keyEvent, readEvent)
  }

  /**
   * Verifies reading and writing of both forms of SysEx event supported
   * by [SysExEvent].
   *
   * The first event represents a normal F0 SysEx event, while the second
   * represents an escaped F7 event.
   *
   * Both events are serialized by [MidiWriter] and then parsed again by
   * [MidiReader]. The resulting events must preserve their raw payload,
   * delta time, and escaped state.
   */
  @Test
  fun `should read SysEx byte sequence events`() {
    val sysEx1 = SysExEvent(
      deltaTime = 10,
      rawData = byteArrayOf(
        0x43,
        0x12,
        0x00
      ),
      escaped = false
    )

    val sysEx2 = SysExEvent(
      deltaTime = 0,
      rawData = byteArrayOf(
        0xFF.toByte(),
        0x2E
      ),
      escaped = true
    )

    val track = listOf(
      sysEx1,
      sysEx2,
      EndOfTrackMetaEvent(0)
    )

    val midi = Midi(
      format = 0,
      division = 240,
      tracks = listOf(track)
    )

    val bytes = MidiWriter.toBytes(midi)
    val readBack = MidiReader.fromBytes(bytes)

    assertEquals(sysEx1, readBack.tracks[0][0])
    assertEquals(sysEx2, readBack.tracks[0][1])
  }

  /**
   * Verifies preservation of unsupported and sequencer-specific meta events.
   *
   * The reader must not discard meta events that it does not interpret into
   * a dedicated event class. Instead, unsupported event types are represented
   * by [UnknownMetaEvent] while the standard Sequencer Specific event
   * (0x7F) is represented by [SequencerSpecificMetaEvent].
   *
   * This test also verifies that the raw payload bytes survive a complete
   * Writer -> Reader round trip.
   */
  @Test
  fun `should read unknown and sequencer specific meta events`() {
    val unknownMeta = UnknownMetaEvent(
      deltaTime = 0,
      type = 0x7E,
      rawData = byteArrayOf(
        0xDE.toByte(),
        0xAD.toByte()
      )
    )

    val seqSpec = SequencerSpecificMetaEvent(
      deltaTime = 5,
      rawData = byteArrayOf(
        0xBE.toByte(),
        0xEF.toByte()
      )
    )

    val track = listOf(
      unknownMeta,
      seqSpec,
      EndOfTrackMetaEvent(0)
    )

    val midi = Midi(
      format = 0,
      division = 100,
      tracks = listOf(track)
    )

    val readBack = MidiReader.fromBytes(
      MidiWriter.toBytes(midi)
    )

    assertEquals(unknownMeta, readBack.tracks[0][0])
    assertEquals(seqSpec, readBack.tracks[0][1])
  }
}