@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

import com.lucasalfare.flmidi.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


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

  @Test
  fun `test header signatures`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = "MThd", midi0.header.signature)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = "MThd", midi1.header.signature)
  }

  @Test
  fun `test header lengths`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 6, midi0.header.length)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 6, midi1.header.length)
  }

  @Test
  fun `test header formats`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 0, midi0.header.format)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 1, midi1.header.format)
  }

  @Test
  fun `test header number of tracks`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 1, midi0.header.numTracks)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 4, midi1.header.numTracks)
  }

  @Test
  fun `test header time division resolutions`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = 96, midi0.header.division)

    val midi1 = readMidiFromBytes(format1Midi)
    assertEquals(expected = 96, midi1.header.division)
  }

  @Test
  fun `test midiFormat0 track existence`() {
    val midi = readMidiFromBytes(format0Midi)
    assertNotNull(midi.tracks.singleOrNull())
  }

  @Test
  fun `test midiFormat1 tracks existence`() {
    val midi = readMidiFromBytes(format1Midi)
    assertTrue(midi.tracks.isNotEmpty())
    assertEquals(expected = midi.tracks.size, actual = midi.header.numTracks)
  }

  @Test
  fun `test MIDIs tracks signatures`() {
    val midi0 = readMidiFromBytes(format0Midi)
    assertEquals(expected = "MTrk", actual = midi0.tracks.first().signature)

    val midi1 = readMidiFromBytes(format1Midi)
    midi1.tracks.forEach {
      assertEquals(expected = "MTrk", actual = it.signature)
    }
  }

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

  @Test
  fun `test MIDI0 track events count`() {
    val midi0 = readMidiFromBytes(format0Midi)
    val events = midi0.tracks.first().events
    assertTrue(events.isNotEmpty())
    assertTrue(events.size == 14)
  }

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
}