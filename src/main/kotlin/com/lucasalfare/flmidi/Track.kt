package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

data class Track(
  var signature: String = "",
  var length: Long = 0,
  var events: List<Event> = mutableListOf()
) {
  fun define(reader: Reader) {
    signature = reader.readString(4)!!
    length = reader.read4Bytes()
    var previousStatus = 0

    for (i in 0..length) {
      val deltaTime = readVariableLengthValue(reader)
      val nextByte = reader.read1Byte()
      var currentStatus = nextByte

      when (currentStatus) {
        0xFF -> { // META
          val type = reader.read1Byte()
          val event = MetaEvent()
          event.category = EventCategory.Meta
          event.deltaTime = deltaTime
          event.type = getMetaEventTypeByCode(type)
          event.defineData(reader)

          if (event.type == MetaEventType.EndOfTrack) {
            break
          }

          events += event
        }

        0xF0, 0xF7 -> { // Sysex
        }

        else -> { // CONTROL
          // check for a running status
          if (currentStatus ushr 7 == 0) {
            currentStatus = previousStatus
          } else {
            currentStatus = nextByte
            previousStatus = currentStatus
          }

          val event = ControlEvent()
          event.deltaTime = deltaTime
          event.category = EventCategory.Control
          event.type = getControlEventTypeByCode(currentStatus)
          event.targetChannel = currentStatus and 0b1111
          event.defineData(reader)

          events += event
        }
      }
    }
  }
}