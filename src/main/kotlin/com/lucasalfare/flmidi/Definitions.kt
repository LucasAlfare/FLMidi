package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

/**
 * Extends the main reader to contains this custom function.
 *
 * Note that this function could exist directly in the
 * Reader API, however, once this function is really specific
 * to work with MIDI file binaries was chosen to it be implemented
 * in this project. Due to this, it is implemented using the
 * extension functionality, from Kotlin tool.
 */
fun Reader.readVariableLengthValue(): Int {
  val mask = 0b0111_1111
  var resultNumber = 0
  var currentByte: Int

  while (true) {
    currentByte = this.read1Byte()
    resultNumber = (resultNumber shl 7) or (currentByte and mask)
    if ((currentByte ushr 7) == 0) return resultNumber
  }
}

fun writeVariableLengthQuantity(value: Int): ByteArray {
  var buffer = value and 0x7F
  var v = value shr 7
  val out = mutableListOf<Byte>()
  while (v > 0) {
    out.add(0, (buffer or 0x80).toByte())
    buffer = v and 0x7F
    v = v shr 7
  }
  out.add(0, buffer.toByte())
  return out.toByteArray()
}