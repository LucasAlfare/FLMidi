@file:OptIn(ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File

enum class EventCategory {
  Control,
  SystemExclusive,
  Meta
}

open class Event(
  var deltaTime: Int = 0,
  var category: EventCategory = EventCategory.Meta,
  var data: Any = arrayOf<Any>()
)

data class MidiInfo(
  var header: Header = Header(),
  var tracks: List<Track> = mutableListOf()
)

fun readVariableLengthValue(reader: Reader): Int {
  var result = 0
  val mask = 0b0111_1111
  val extractedBytes = mutableListOf<Int>()

  while (true) {
    val currentByte = reader.read1Byte()

    extractedBytes += currentByte and mask

    if ((currentByte ushr 7) == 0) {
      extractedBytes.forEach {
        result = (result shl 7) or it
      }
      return result
    }
  }
}

fun loadAndReadMidiFile(pathname: String): MidiInfo {
  val file = File(pathname)

  if (file.exists() && !file.isDirectory) {
    val fileData = file.readBytes().toUByteArray()
    val reader = Reader(fileData)
    val info = MidiInfo()

    val header = Header()
    header.define(reader)

    info.header = header

    repeat(header.numTracks) {
      val track = Track()
      track.define(reader)

      info.tracks += track
    }

    return info
  }

  return MidiInfo()
}

fun exampleToBeRun() {
  loadAndReadMidiFile("test.mid").tracks.first().events.forEach {
    println(it)
  }
}

fun main() {
  exampleToBeRun()
}