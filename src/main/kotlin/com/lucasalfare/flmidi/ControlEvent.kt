package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

enum class ControlEventType(val typeBytePrefix: Int = -1) {
  Empty,
  NoteOn(0b1001),
  NoteOff(0b1000),
  SelectInstrument(0b1100),

}

class ControlEvent(
  var type: ControlEventType = ControlEventType.Empty,
  var targetChannel: Int = 0
) : Event() {

  fun defineData(reader: Reader) {
    when (this.type) {
      ControlEventType.SelectInstrument -> {
        val targetInstrument = reader.read1Byte()
        this.data = targetInstrument
      }

      ControlEventType.NoteOn -> {
        val noteNumber = reader.read1Byte() and 0b01111111
        val noteVelocity = reader.read1Byte() and 0b01111111
        this.data = listOf(noteNumber, noteVelocity)
      }

      ControlEventType.NoteOff -> {
        val noteNumber = reader.read1Byte() and 0b01111111
        val noteVelocity = reader.read1Byte() and 0b01111111
        this.data = listOf(noteNumber, noteVelocity)
      }

      else -> {
        error("Unhandled control event type: ${this.type}")
      }
    }
  }

  override fun toString(): String {
    return "ControlEvent(type=$type, deltaTime=$deltaTime, targetChannel=$targetChannel, data=$data)"
  }
}

fun getControlEventTypeByCode(typeByte: Int = 0): ControlEventType {
  ControlEventType.values().forEach {
    if (it.typeBytePrefix == (typeByte ushr 4)) {
      return it
    }
  }

  return ControlEventType.Empty
}
