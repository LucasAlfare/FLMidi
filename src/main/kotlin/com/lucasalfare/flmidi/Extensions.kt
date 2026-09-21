// VLQUtils.kt
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import com.lucasalfare.flbinary.Writer

/**
 * Reads a MIDI Variable-Length Quantity (VLQ) from the current reader position.
 *
 * A VLQ is a value encoded using one to four bytes, where each byte contributes
 * seven data bits. The most significant bit (MSB) of each byte is used as a
 * continuation flag:
 *
 * - `0` — this is the last byte of the VLQ.
 * - `1` — another byte follows.
 *
 * The bytes are interpreted in big-endian groups of seven bits. For example,
 * the decimal value `128` is encoded as:
 *
 * ```
 * 0x81 0x00
 * ```
 *
 * The first byte contains the continuation flag (`0x80`) and the upper seven
 * bits of the value, while the second byte contains the lower seven bits.
 *
 * MIDI Standard MIDI Files (SMF) limit VLQs to a maximum of four bytes,
 * allowing values from `0` through `0x0FFFFFFF` (268,435,455).
 *
 * This function advances the reader position by the number of bytes consumed.
 *
 * ### Example
 *
 * Given the following bytes:
 *
 * ```
 * 0x81 0x00
 * ```
 *
 * calling:
 *
 * ```
 * val value = reader.readVariableLengthValue()
 * ```
 *
 * returns:
 *
 * ```
 * 128
 * ```
 *
 * ### Why this exists
 *
 * MIDI files use VLQs extensively for values whose size can vary depending
 * on their magnitude, particularly for delta-times inside MIDI tracks.
 * Smaller values therefore occupy fewer bytes, keeping the file compact.
 *
 * @return the decoded VLQ value as an [Int].
 *
 * @throws IllegalArgumentException if the VLQ requires more than four bytes.
 * @throws IllegalArgumentException if the reader reaches the end of its data
 * before a complete VLQ can be read.
 */
fun Reader.readVariableLengthValue(): Int {
  val mask = 0b0111_1111
  var resultNumber = 0
  var currentByte: Int

  // A MIDI VLQ may contain at most four bytes.
  // Each byte contributes seven bits to the resulting value.
  for (i in 1..4) {
    require(this.position < this.data.size) {
      "Unexpected end of data while reading VLQ"
    }

    // Read the next byte from the binary stream.
    currentByte = this.read1Byte()

    // Remove the continuation flag (MSB) and append the remaining
    // seven data bits to the value accumulated so far.
    //
    // Shifting by seven is equivalent to making room for the
    // seven bits contained in the newly read byte.
    resultNumber = (resultNumber shl 7) or (currentByte and mask)

    // If the MSB is not set, this byte is the final byte of the VLQ.
    if ((currentByte and 0x80) == 0) {
      return resultNumber
    }
  }

  // Reaching this point means that all four allowed bytes had their
  // continuation bit set, which would require a fifth byte.
  throw IllegalArgumentException("VLQ exceeded 4 bytes")
}

/**
 * Encodes an integer as a MIDI Variable-Length Quantity (VLQ).
 *
 * A VLQ stores a value using groups of seven bits. The most significant bit
 * of each byte is reserved as a continuation flag:
 *
 * - `0` — this is the final byte.
 * - `1` — another byte follows.
 *
 * MIDI Standard MIDI Files (SMF) allow VLQs to contain at most four bytes.
 * Consequently, the largest representable value is `0x0FFFFFFF`
 * (268,435,455).
 *
 * The encoding process starts with the seven least significant bits of the
 * value and then processes the remaining groups of seven bits from right to
 * left. Every byte except the final byte receives the continuation flag
 * (`0x80`).
 *
 * ### Examples
 *
 * The value `0` is encoded as:
 *
 * ```
 * 0x00
 * ```
 *
 * The value `127` is encoded as:
 *
 * ```
 * 0x7F
 * ```
 *
 * The value `128` requires two bytes:
 *
 * ```
 * 0x81 0x00
 * ```
 *
 * The value `255` is encoded as:
 *
 * ```
 * 0x81 0x7F
 * ```
 *
 * ### Example usage
 *
 * ```
 * val encoded = writer.writeVariableLengthQuantity(128)
 * ```
 *
 * The resulting [ByteArray] contains:
 *
 * ```
 * [0x81, 0x00]
 * ```
 *
 * ### Why this exists
 *
 * MIDI files frequently contain values that are usually small but can
 * occasionally become larger. VLQs allow small values to occupy fewer bytes
 * while still supporting much larger values when necessary.
 *
 * One of the most common uses of MIDI VLQs is encoding delta-time values
 * inside track events.
 *
 * @param value the non-negative integer to encode. Must be between `0` and
 * `0x0FFFFFFF`, inclusive.
 *
 * @return a [ByteArray] containing the MIDI VLQ representation of [value].
 *
 * @throws IllegalArgumentException if [value] is outside the range supported
 * by the MIDI VLQ format.
 */
fun Writer.writeVariableLengthQuantity(value: Int): ByteArray {
  require(value in 0..0x0FFFFFFF) {
    "VLQ value out of range: $value"
  }

  var remaining = value
  val bytes = mutableListOf<Byte>()

  // Store the seven least significant bits first.
  //
  // This byte is always the final byte of the VLQ, so its continuation
  // flag remains unset.
  bytes.add((remaining and 0x7F).toByte())

  remaining = remaining shr 7

  // Process the remaining groups of seven bits.
  //
  // New bytes are inserted at the beginning because the groups are
  // extracted from least significant to most significant, while the
  // MIDI VLQ format stores them in most-significant-first order.
  while (remaining > 0) {
    bytes.add(
      0,
      ((remaining and 0x7F) or 0x80).toByte()
    )

    remaining = remaining shr 7
  }

  return bytes.toByteArray()
}