package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import kotlin.math.pow


enum class MetaEventType(var typeByte: Int = -1) {
  Empty,
  SequenceNumber(0x00),
  TextEvent(0x01),
  CopyrightNotice(0x02),
  TrackName(0x03),
  InstrumentName(0x04),
  Lyric(0x05),
  Marker(0x06),
  CuePoint(0x07),
  MidiChannelPrefix(0x20),
  EndOfTrack(0x2f),
  SetTempoInMicrosecondsPerQuarterNote(0x51),
  SmpteOffset(0x54),
  TimeSignature(0x58),
  KeySignature(0x59),
  SequencerSpecificMetaEvent(0x7f)
}

class MetaEvent(
  var type: MetaEventType = MetaEventType.Empty
) : Event() {

  fun defineData(reader: Reader) {
    when (this.type) {
      MetaEventType.TextEvent,
      MetaEventType.CopyrightNotice,
      MetaEventType.TrackName,
      MetaEventType.InstrumentName,
      MetaEventType.Lyric,
      MetaEventType.Marker,
      MetaEventType.CuePoint -> {
        val textLength = readVariableLength(reader)
        this.data = reader.readString(textLength)!!
      }

      MetaEventType.TimeSignature -> {
        val numDataItems = reader.read1Byte()

        val upperSignatureValue = reader.read1Byte()
        val powerOfTwoToLowerValue = reader.read1Byte()
        val nMidiClocksInMetronomeClick = reader.read1Byte()
        val numberOf32ndNotesIn24MidiClocks = reader.read1Byte()

        this.data = listOf(
          upperSignatureValue,
          2f.pow(powerOfTwoToLowerValue).toInt(),
          nMidiClocksInMetronomeClick,
          numberOf32ndNotesIn24MidiClocks
        )
      }

      MetaEventType.SetTempoInMicrosecondsPerQuarterNote -> {
        val numDataItems = reader.read1Byte()
        val tempoInMicroseconds = reader.read3Bytes()
        this.data = tempoInMicroseconds
      }

      MetaEventType.SmpteOffset -> {
        val dataLength = reader.read1Byte()
        this.data = listOf(
          reader.read1Byte(),
          reader.read1Byte(),
          reader.read1Byte(),
          reader.read1Byte(),
          reader.read1Byte()
        )
      }

      MetaEventType.KeySignature -> {
        val dataLength = reader.read1Byte()
        this.data = listOf(reader.read1Byte(), reader.read1Byte())
      }

      MetaEventType.EndOfTrack -> {
        val dataLength = reader.read1Byte()
        this.data = "[no data]"
      }

      MetaEventType.MidiChannelPrefix -> {
        val dataLength = reader.read1Byte()
        val currentEffectiveMidiChannel = reader.read1Byte()
        this.data = currentEffectiveMidiChannel
      }

      MetaEventType.SequencerSpecificMetaEvent -> {
        val dataLength = readVariableLength(reader)
        val auxBytes = mutableListOf<Int>()
        repeat(dataLength) {
          auxBytes += reader.read1Byte()
        }
        this.data = auxBytes
      }

      else -> {
        error("Unhandled meta event type: ${this.type}[${this.type.typeByte}]")
      }
    }
  }

  override fun toString(): String {
    return "MetaEvent(type=$type, deltaTime=$deltaTime, data=$data)"
  }
}

fun getMetaEventTypeByCode(typeByte: Int = 0): MetaEventType {
  MetaEventType.values().forEach {
    if (it.typeByte == typeByte) {
      return it
    }
  }

  return MetaEventType.Empty
}