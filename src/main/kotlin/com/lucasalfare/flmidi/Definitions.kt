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

@OptIn(ExperimentalUnsignedTypes::class)
fun loadAndReadMidiFile(pathname: String): MidiInfo {
  val file = File(pathname)

  if (file.exists() && !file.isDirectory) {
    val fileData = file.readBytes().toUByteArray()

    if (fileData.isNotEmpty()) {
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
    } else {
      throw FLMidiException("The file of the passed path is empty of data.")
    }
  }

  throw FLMidiException("Could not to read the supplied file at the specified path.")
}