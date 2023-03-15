package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

enum class ControlEventType(val typeBytePrefix: Int = -1) {
  Empty,

  NoteOff(0b1000),
  NoteOn(0b1001),
  KeyPressure(0b1010),

  ChannelMode(0b1011),

  SelectInstrument(0b1100),
  ChannelPressure(0b1101),
  PitchBend(0b1110)
}

enum class ChannelModeType(val typeByteSuffix: Int = -1) {
  Empty,
  BankSelect(0b00000000),
  ModulationWheel(0b00000001),
  BreathController(0b00000010),
  FootController(0b00000100),
  PortamentoTime(0b00000101),
  DataEntrySliderMSB(0b00000110),
  MainVolume(0b00000111),
  Balance(0b00001000),
  Pan(0b00001010),
  ExpressionController(0b00001011),
  EffectControl1(0b00001100),
  EffectControl2(0b00001101),
  GeneralPurposeController1(0b00010000),
  GeneralPurposeController2(0b00010001),
  GeneralPurposeController3(0b00010010),
  GeneralPurposeController4(0b00010011),
  //TODO LSB for controllers 0..31
  DamperPedal(0b01000000),
  Portamento(0b01000001),
  SostenatoPedal(0b01000010),
  SoftPedal(0b01000011),
  LegatoFootswitch(0b01000100),
  Hold2(0b01000101)
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

      ControlEventType.ChannelMode -> {
        val channelModeTypeCode = reader.read1Byte()
        val channelModeArg1 = reader.read1Byte()
        val channelModeArg2 = reader.read1Byte()
        this.data = listOf(
          channelModeTypeCode,
          channelModeArg1,
          channelModeArg2
        )
      }

      ControlEventType.ChannelPressure -> {
        val channelPressure = reader.read1Byte()
        this.data = channelPressure
      }

      ControlEventType.PitchBend -> {
        val pitchBend = reader.read1Byte()
        this.data = pitchBend
      }

      else -> {
        error("Unhandled control event type: ${this.type}[${this.type.typeBytePrefix.toBinaryString()}]")
      }
    }
  }

  override fun toString(): String {
    return "ControlEvent(type=$type, deltaTime=$deltaTime, targetChannel=$targetChannel, data=$data)"
  }
}

fun getControlEventTypeByCode(typeByte: Int = 0): ControlEventType {
  ControlEventType.values().forEach {
    if ((typeByte ushr 4) == it.typeBytePrefix) {
      return it
    }
  }

  return ControlEventType.Empty
}
