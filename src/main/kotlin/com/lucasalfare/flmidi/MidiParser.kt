@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.math.pow

// Tipos de eventos gerais
const val META_EVENT = 0xFF
const val SYSTEM_EXCLUSIVE_EVENT = 0xF0
const val SYSTEM_EXCLUSIVE_ESCAPE_EVENT = 0xF7

// Tipos de meta-eventos
const val SEQUENCE_NUMBER = 0x00
const val TEXT_EVENT = 0x01
const val COPYRIGHT_NOTICE = 0x02
const val TRACK_NAME = 0x03
const val INSTRUMENT_NAME = 0x04
const val LYRIC = 0x05
const val MARKER = 0x06
const val CUE_POINT = 0x07
const val MIDI_CHANNEL_PREFIX = 0x20
const val END_OF_TRACK = 0x2F
const val SET_TEMPO = 0x51
const val SMPTE_OFFSET = 0x54
const val TIME_SIGNATURE = 0x58
const val KEY_SIGNATURE = 0x59
const val SEQUENCER_SPECIFIC = 0x7F

// Tipos de eventos de controle (usando apenas os 4 bits mais significativos)
const val NOTE_OFF = 0b1000
const val NOTE_ON = 0b1001
const val POLYPHONIC_KEY_PRESSURE = 0b1010
const val CONTROL_CHANGE = 0b1011
const val PROGRAM_CHANGE = 0b1100
const val CHANNEL_PRESSURE = 0b1101
const val PITCH_BEND = 0b1110

open class Event

data class MetaEvent(
  val eventType: Int,
  val deltaTime: Int,
  val data: Any
) : Event()

data class ControlEvent(
  val type: Int,
  val delta: Int,
  val data: Any,
  val targetChannel: Int
) : Event()

data class Header(
  val chunkType: String,
  val length: Long,
  val format: Int,
  val numTracks: Int,
  val division: Int
) {
  init {
    require(chunkType == "MThd") { "Header chunk type signature is not 'MThd'!" }
    if (format == 0) require(numTracks == 1)
    else if (format == 1 || format == 2) require(numTracks >= 1)
  }
}

data class Track(
  val type: String,
  val length: Int,
  val events: List<Event>
) {
  init {
    require(type == "MTrk") { "Track type signature is not 'MTrk'!" }
    require(length > 0) { "Track with length 0!" }
    require(events.isNotEmpty()) { "Track without any events!" }
  }
}

data class Midi(
  val header: Header,
  val tracks: List<Track>
)

fun readMetaEvent(reader: Reader, deltaTime: Int): MetaEvent {
  val type = reader.read1Byte()
  when (type) {
    SEQUENCE_NUMBER -> {
      val dataLength = reader.readVariableLengthValue()
      val sequenceNumber = reader.read2Bytes()
      return MetaEvent(type, deltaTime, sequenceNumber)
    }

    TEXT_EVENT, COPYRIGHT_NOTICE, TRACK_NAME, INSTRUMENT_NAME, LYRIC, MARKER, CUE_POINT -> {
      val textLength = reader.readVariableLengthValue()
      val data = reader.readString(textLength) ?: ""
      return MetaEvent(type, deltaTime, data)
    }

    TIME_SIGNATURE -> {
      val numDataItems = reader.readVariableLengthValue()
      val upperSignatureValue = reader.read1Byte()
      val powerOfTwoToLowerValue = reader.read1Byte()
      val nMidiClocksInMetronomeClick = reader.read1Byte()
      val numberOf32ndNotesIn24MidiClocks = reader.read1Byte()
      val data = listOf(
        upperSignatureValue,
        2f.pow(powerOfTwoToLowerValue).toInt(),
        nMidiClocksInMetronomeClick,
        numberOf32ndNotesIn24MidiClocks
      )
      return MetaEvent(type, deltaTime, data)
    }

    SET_TEMPO -> {
      val numDataItems = reader.readVariableLengthValue()
      val tempoInMicroseconds = reader.read3Bytes()
      return MetaEvent(type, deltaTime, tempoInMicroseconds)
    }

    SMPTE_OFFSET -> {
      val dataLength = reader.readVariableLengthValue()
      val data = listOf(
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte(),
        reader.read1Byte()
      )
      return MetaEvent(type, deltaTime, data)
    }

    KEY_SIGNATURE -> {
      val dataLength = reader.readVariableLengthValue()
      val data = listOf(reader.read1Byte(), reader.read1Byte())
      return MetaEvent(type, deltaTime, data)
    }

    MIDI_CHANNEL_PREFIX -> {
      val dataLength = reader.readVariableLengthValue()
      val currentEffectiveMidiChannel = reader.read1Byte()
      return MetaEvent(type, deltaTime, currentEffectiveMidiChannel)
    }

    SEQUENCER_SPECIFIC -> {
      val dataLength = reader.readVariableLengthValue()
      val auxBytes = mutableListOf<Int>()
      repeat(dataLength) { auxBytes += reader.read1Byte() }
      return MetaEvent(type, deltaTime, auxBytes)
    }

    END_OF_TRACK -> {
      reader.readVariableLengthValue().also { require(it == 0) }
      return MetaEvent(type, deltaTime, emptyList<Int>())
    }

    else -> {
      println("Evento meta desconhecido encontrado: [$type]. Lendo mesmo assim...")
      val dataLength = reader.readVariableLengthValue()
      repeat(dataLength) { reader.read1Byte() }
      return MetaEvent(type, deltaTime, emptyList<Int>())
    }
  }
}

fun readControlEvent(reader: Reader, deltaTime: Int, status: Int): ControlEvent {
  val channel = status and 0b1111
  val type = status shr 4
  when (type) {
    PROGRAM_CHANGE -> {
      val targetInstrument = reader.read1Byte()
      return ControlEvent(type, deltaTime, targetInstrument, channel)
    }
    NOTE_ON, NOTE_OFF -> {
      val noteNumber = reader.read1Byte() and 0b01111111
      val noteVelocity = reader.read1Byte() and 0b01111111
      val data = listOf(noteNumber, noteVelocity)
      return ControlEvent(type, deltaTime, data, channel)
    }
    POLYPHONIC_KEY_PRESSURE -> {
      val noteNumber = reader.read1Byte()
      val pressure = reader.read1Byte()
      val data = listOf(noteNumber, pressure)
      return ControlEvent(type, deltaTime, data, channel)
    }
    CONTROL_CHANGE -> {
      val controlNumber = reader.read1Byte()
      val controlValue = reader.read1Byte()
      val data = listOf(controlNumber, controlValue)
      return ControlEvent(type, deltaTime, data, channel)
    }
    CHANNEL_PRESSURE -> {
      val channelPressure = reader.read1Byte()
      return ControlEvent(type, deltaTime, channelPressure, channel)
    }
    PITCH_BEND -> {
      val lsb = reader.read1Byte()
      val msb = reader.read1Byte()
      val pitchBend = (msb shl 7) or lsb
      return ControlEvent(type, deltaTime, pitchBend, channel)
    }
    else -> error("Tipo de evento de controle desconhecido: ${type.toString(16)}")
  }
}

fun readMidi(pathname: String): Midi {
  val file = File(pathname)
  if (!file.exists()) error("Arquivo não existe")
  if (file.isDirectory) error("Caminho [$pathname] é um diretório, não um arquivo")

  val fileBytes = file.readBytes().toUByteArray()
  val reader = Reader(fileBytes)

  // Lê o header
  val header = Header(
    chunkType = reader.readString(4) ?: error("Sem assinatura do tipo de chunk do header!"),
    length = reader.read4Bytes(),
    format = reader.read2Bytes(),
    numTracks = reader.read2Bytes(),
    division = reader.read2Bytes()
  )

  // Lê os tracks
  val tracks = mutableListOf<Track>()
  repeat(header.numTracks) {
    val trackType = reader.readString(4) ?: error("Sem assinatura do tipo de chunk do track!")
    val trackLength = reader.read4Bytes().toInt()
    val finalOffset = reader.position + trackLength
    val events = mutableListOf<Event>()
    var previousStatus = 0

    while (reader.position < finalOffset) {
      val currentDeltaTime = reader.readVariableLengthValue()
      var currentStatus = reader.read1Byte()

      // Suporte ao "running status"
      if (currentStatus ushr 7 == 0) {
        currentStatus = previousStatus
        reader.position--
      }

      when (currentStatus) {
        META_EVENT -> {
          val metaEvent = readMetaEvent(reader, currentDeltaTime)
          events += metaEvent
          if (metaEvent.eventType == END_OF_TRACK) break
          previousStatus = 0 // Reset após meta-evento
        }
        SYSTEM_EXCLUSIVE_EVENT, SYSTEM_EXCLUSIVE_ESCAPE_EVENT -> {
          val length = reader.readVariableLengthValue()
          repeat(length) { reader.read1Byte() }
          println("Evento SysEx encontrado! Pulando $length byte(s)...")
          previousStatus = 0 // Reset após SysEx
        }
        else -> {
          previousStatus = currentStatus
          val controlEvent = readControlEvent(reader, currentDeltaTime, currentStatus)
          events += controlEvent
        }
      }
    }

    tracks += Track(type = trackType, length = trackLength, events = events)
  }

  return Midi(header = header, tracks = tracks)
}

fun main() {
  val midi = readMidi("example.mid")
  midi.tracks.first().events.forEach { println(it) }
}