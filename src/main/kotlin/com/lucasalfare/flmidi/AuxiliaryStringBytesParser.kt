@file:OptIn(ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import java.io.File

/**
 * Just parses the string bytes from online specification
 * and writes then into a file.
 */
fun main() {
  val s = "4D 54 68 64 00 00 00 06 00 00 00 01 00 60 4D 54 72 6B 00 00 00 3B 00 FF 58 04 04 02 18 08 00 FF 51 03 07 A1 20 00 C0 05 00 C1 2E 00 C2 46 00 92 30 60 00 3C 60 60 91 43 40 60 90 4C 20 81 40 82 30 40 00 3C 40 00 81 43 40 00 80 4C 40 00 FF 2F 00"
    .replace("\t", "")
    .replace("\n", " ")
    .replace("  ", " ")
    .trim()
    .split(" ")
  println(s)

  val bytes = UByteArray(s.size)
  s.forEachIndexed { index, b ->
    if (b.isNotEmpty()) {
      bytes[index] = Integer.parseInt(b, 16).toUByte()
    }
  }

  val f = File("example.mid")
  f.writeBytes(bytes.toByteArray())
}