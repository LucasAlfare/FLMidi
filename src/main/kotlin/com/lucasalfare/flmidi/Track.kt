package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

data class Track(
  var signature: String = "",
  var length: Long = 0,
  var events: List<Event> = mutableListOf()
) {

  /**
   * Main function to define (read) the "current" track
   * information from the raw file bytes.
   */
  fun define(reader: Reader) {
    signature = reader.readString(4)!!
    length = reader.read4Bytes()
    var previousStatus = 0

    while (true) {
      val deltaTime = reader.readVariableLengthValue()
      var currentStatus = reader.read1Byte()

      /*
      If this condition is [true] then we are this current
      byte is part of a sequence of a "running status" mode.

      This means that all next stats/events will be the
      same until this condition turns to [false].

      Also, when in a running status the actual current
      byte will not be representing a control event. Instead,
      it should be part of the next information. So due this
      case we should "backtrack" the current reading position
      to the previous one.

      This will make the reading sequence works correctly,
      respecting the running status specification.
       */
      if (currentStatus ushr 7 == 0) {
        currentStatus = previousStatus
        reader.position--
      }

      when (currentStatus) {
        0xFF -> { // META
          val type = reader.read1Byte()
          val event = MetaEvent()
          event.category = EventCategory.Meta
          event.deltaTime = deltaTime
          event.type = getMetaEventTypeByCode(type)
          event.defineData(reader)

          events += event

          if (event.type == MetaEventType.EndOfTrack) {
            break
          }
        }

        0xF0, 0xF7 -> { /* TODO: Sysex */ }

        else -> { // CONTROL
          // updates running status "mode"
          previousStatus = currentStatus

          val event = ControlEvent()
          event.deltaTime = deltaTime
          event.category = EventCategory.Control
          event.type = getControlEventTypeByCode(currentStatus)

          /*
          The channel information lives in the first four bits.
          To extract then, just mask the value with 0b1111.
           */
          event.targetChannel = currentStatus and 0b1111
          event.defineData(reader)

          events += event
        }
      }
    }
  }
}