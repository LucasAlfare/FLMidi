@file:OptIn(ExperimentalUnsignedTypes::class)

import com.lucasalfare.flbinary.Reader
import com.lucasalfare.flmidi.loadAndReadMidiFile
import com.lucasalfare.flmidi.readVariableLengthValue
import kotlin.test.Test
import kotlin.test.assertEquals

class GeneralTest {

  @OptIn(ExperimentalUnsignedTypes::class)
  @Test
  fun `test readVariableLength()`() {
    val arrays = arrayOf(
      arrayOf(0x00, byteArrayOf(0x00)),
      arrayOf(0x3A, byteArrayOf(0x3A)),
      arrayOf(0x40, byteArrayOf(0x40)),
      arrayOf(0x7f, byteArrayOf(0x7f)),
      arrayOf(0x80, byteArrayOf(0x81.toByte(), 0x00)),
      arrayOf(0xae, byteArrayOf(0x81.toByte(), 0x2e)),
      arrayOf(0xf0, byteArrayOf(0x81.toByte(), 0x70)),
      arrayOf(0x12c, byteArrayOf(0x82.toByte(), 0x2c)),
      arrayOf(0x2000, byteArrayOf(0xc0.toByte(), 0x00)),
      arrayOf(0x4000, byteArrayOf(0x81.toByte(), 0x80.toByte(), 0x00)),
      arrayOf(0x100000, byteArrayOf(0xc0.toByte(), 0x80.toByte(), 0x00))
    )

    arrays.forEach {
      val expected = it[0] as Int
      val data = it[1] as ByteArray
      val reader = Reader(data.toUByteArray())
      assertEquals(expected, reader.readVariableLengthValue())
    }
  }

  @Test
  fun exampleToBeRun() {
    val info = loadAndReadMidiFile("example.mid")
    println("Header info:\n\t${info.header}")
    println("-------- -------- -------- --------")
    info.tracks.forEachIndexed { _, track ->
      println("Current track info:\n\t${track}")
      println("--> Events of this track:")
      track.events.forEach {
        println("\t$it")
      }
    }

    assertEquals(1, 1)
  }
}