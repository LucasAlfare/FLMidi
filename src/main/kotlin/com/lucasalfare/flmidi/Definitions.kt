package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

/**
 * Reads a variable-length quantity (VLQ) from the current position in the binary stream.
 *
 * ---
 * ### What is a Variable-Length Quantity (VLQ)?
 * In the Standard MIDI File (SMF) format, several numeric values (such as `deltaTime`) are encoded using
 * a special format called "Variable-Length Quantity" (VLQ). Instead of using a fixed number of bytes,
 * a VLQ uses 1 to 4 bytes depending on the value. This optimizes space, especially for smaller numbers.
 *
 * Each byte in a VLQ uses 7 bits for data and 1 bit (the most significant bit) as a continuation flag:
 * - If the highest bit (`0x80`) is **1**, the next byte is part of the value.
 * - If the highest bit is **0**, it indicates the **last byte** of the value.
 *
 * ---
 * ### Function Purpose
 * This function is designed specifically to decode such variable-length values from raw MIDI binary data.
 * While this could theoretically belong to a generic `Reader` API, it is highly specific to the MIDI format,
 * so it's implemented here as an **extension function** for better modularity and separation of concerns.
 *
 * ---
 * ### Implementation Details
 * - `mask = 0b0111_1111`: Used to extract only the 7 data bits of each byte.
 * - `resultNumber`: Accumulates the final integer value.
 * - The function keeps shifting the result left by 7 bits and OR-ing the next 7-bit segment.
 * - It loops until it finds a byte with the MSB (most significant bit) **unset**, signaling the end of the VLQ.
 *
 * ---
 * @receiver Reader
 * The binary reader instance currently positioned at the start of a variable-length quantity.
 *
 * @return Int
 * The decoded integer value from the VLQ.
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