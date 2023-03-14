package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

enum class EventCategory {
  Control,
  SystemExclusive,
  Meta
}

data class Header(
  var signature: String = "",
  var length: Long = 0,
  var midiFormat: Int = 0,
  var numTracks: Int = 0,
  var timeDivision: Int = 0
)

data class Track(
  var signature: String = "",
  var length: Long = 0,
  var events: List<Event> = mutableListOf()
)

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