@file:OptIn(ExperimentalUnsignedTypes::class)

import com.lucasalfare.flbinary.Reader
import com.lucasalfare.flmidi.readVariableLength
import kotlin.test.*

class GeneralTest {

  @Test
  fun `test readVariableLength()`() {
    val bytes = byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f)
    val reader = Reader(bytes.toUByteArray())
    assertEquals(0xfffffff, readVariableLength(reader))
  }
}