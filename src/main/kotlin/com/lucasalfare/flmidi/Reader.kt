package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.math.pow

//open class Event
//
//data class MetaEvent(
//  val type: Int,
//  val delta: Int,
//  val data: Any? = null
//) : Event()
//
//data class ControlEvent(
//  val type: Int,
//  val delta: Int,
//  val data: Any? = null,
//  val targetChannel: Int = type and 0b1111
//) : Event()
//
//data class Track(
//  val signature: String,
//  val length: Long,
//  val events: MutableList<Event>
//)
//
//data class Header(
//  val signature: String,
//  val length: Long,
//  val midiFormat: Int,
//  val numTracks: Int,
//  val timeDivision: Int
//)
//
//data class Midi(val header: Header, val tracks: List<Track>)
//
//@OptIn(ExperimentalUnsignedTypes::class)
//fun parse(pathname: String): Midi {
//  val file = File(pathname)
//  if (!file.exists() || file.isDirectory) throw Throwable("Invalid input file.")
//  val reader = Reader(file.readBytes().toUByteArray()) // Reader é auxiliar, só pra me ajudar a ler os Bytes!
//  val header = Header(
//    signature = reader.readString(4) ?: error("no header signature"),
//    length = reader.read4Bytes(),
//    midiFormat = reader.read2Bytes(),
//    numTracks = reader.read2Bytes(),
//    timeDivision = reader.read2Bytes()
//  )
//  val tracks = mutableListOf<Track>()
//  for (i in 0..<header.numTracks) {
//    val trackSignature = reader.readString(4)
//      ?: error("no track signature") // uma das tracks de um dos meus arquivos de exemplo retorna ' MTr'!
//    val trackLength = reader.read4Bytes()
//    var previousStatus = 0
//    val trackEvents = mutableListOf<Event>()
//    while (true) {
//      val delta = reader.readVariableLengthValue()
//      var currentStatus = reader.read1Byte()
//      if (currentStatus ushr 7 == 0) {
//        currentStatus = previousStatus
//        reader.position--
//      }
//      when (currentStatus) {
//        0xFF -> {
//          when (val type = reader.read1Byte()) {
//            // text event, copyright notice, track name, instrument name, lyric, marker, cue point
//            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 -> {
//              val textLength = reader.readVariableLengthValue()
//              val data = reader.readString(textLength) ?: ""
//              trackEvents += MetaEvent(type, delta, data)
//            }
//
//            // time signature
//            0x58 -> {
////              val numDataItems = reader.read1Byte()
//              val numDataItems = reader.readVariableLengthValue()
//
//              val upperSignatureValue = reader.read1Byte()
//              val powerOfTwoToLowerValue = reader.read1Byte()
//              val nMidiClocksInMetronomeClick = reader.read1Byte()
//              val numberOf32ndNotesIn24MidiClocks = reader.read1Byte()
//
//              val data = listOf(
//                upperSignatureValue,
//                2f.pow(powerOfTwoToLowerValue).toInt(),
//                nMidiClocksInMetronomeClick,
//                numberOf32ndNotesIn24MidiClocks
//              )
//
//              trackEvents += MetaEvent(type, delta, data)
//            }
//
//            // set tempo in microseconds per quarter note
//            0x51 -> {
////              val numDataItems = reader.read1Byte() // reader.readVariableLengthValue()
//              val numDataItems = reader.readVariableLengthValue()
//              val tempoInMicroseconds = reader.read3Bytes()
//              trackEvents += MetaEvent(type, delta, tempoInMicroseconds)
//            }
//
//            // SMPTE Offset
//            0x54 -> {
//              val dataLength = reader.readVariableLengthValue()
//              val data = listOf(
//                reader.read1Byte(),
//                reader.read1Byte(),
//                reader.read1Byte(),
//                reader.read1Byte(),
//                reader.read1Byte()
//              )
//
//              trackEvents += MetaEvent(type, delta, data)
//            }
//
//            // key signature
//            0x59 -> {
//              val dataLength = reader.readVariableLengthValue()
//              val data = listOf(reader.read1Byte(), reader.read1Byte())
//              trackEvents += MetaEvent(type, delta, data)
//            }
//
//            // midi channel prefix
//            0x20 -> {
//              val dataLength = reader.readVariableLengthValue()
//              val currentEffectiveMidiChannel = reader.read1Byte()
//              trackEvents += MetaEvent(type, delta, currentEffectiveMidiChannel)
//            }
//
//            // sequencer specific meta event
//            0x7f -> {
//              val dataLength = reader.readVariableLengthValue()
//              val auxBytes = mutableListOf<Int>()
//              repeat(dataLength) {
//                auxBytes += reader.read1Byte()
//              }
//
//              trackEvents += MetaEvent(type, delta, auxBytes)
//            }
//
//            // end of track
//            0x2F -> {
//              reader.read1Byte()
//              trackEvents += MetaEvent(type, delta)
//              break
//            }
//
//            else -> {
////              error("Unknown track meta event type: ${type.toHexString()}")
//              val dataLength = reader.readVariableLengthValue()
//              val data = mutableListOf<Int>()
//              repeat(dataLength) {
//                data += reader.read1Byte()
//              }
//              trackEvents += MetaEvent(type, delta, data)
//            }
//          }
//          previousStatus = 0
//        }
//
//        0xF0, 0xF7 -> {
//          val length = reader.readVariableLengthValue()
//          repeat(length) { reader.read1Byte() }
//          println("Reached a SysEx event! Skipped $length byte(s)...")
//          previousStatus = 0 // Reset do running status após SysEx IA
//        }
//
//        else -> {
//          // updates running status "mode"
//          previousStatus = currentStatus
//          when (val type = currentStatus shr 4) {
//            // select instrument
//            0b1100 -> {
//              val targetInstrument = reader.read1Byte()
//              trackEvents += ControlEvent(type, delta, targetInstrument)
//            }
//
//            // note on, note off
//            0b1001, 0b1000 -> {
//              val noteNumber = reader.read1Byte() and 0b01111111
//              val noteVelocity = reader.read1Byte() and 0b01111111
//              val data = listOf(noteNumber, noteVelocity)
//              trackEvents += ControlEvent(type, delta, data)
//            }
//
//            // Polyphonic Key Pressure (After touch).
//            0b1010 -> {
//              val noteNumber = reader.read1Byte()
//              val pressure = reader.read1Byte()
//              trackEvents += ControlEvent(type, delta, listOf(noteNumber, pressure))
//            }
//
//            // channel mode
//            0b1011 -> {
//              val controlNumber = reader.read1Byte()
//              val controlValue = reader.read1Byte()
//              val data = listOf(controlNumber, controlValue)
//              trackEvents += ControlEvent(type, delta, data)
//            }
//
//            // channel pressure
//            0b1101 -> {
//              val channelPressure = reader.read1Byte()
//              trackEvents += ControlEvent(type, delta, channelPressure)
//            }
//
//            // pitch bend
//            0b1110 -> {
//              val lsb = reader.read1Byte()
//              val msb = reader.read1Byte()
//              val pitchBend = (msb shl 7) or (lsb)
//              trackEvents += ControlEvent(type, delta, pitchBend)
//            }
//
//            else -> {
//              error("Unknown track control event type: ${type.toHexString()}")
//            }
//          }
//        }
//      }
//    }
//
//    tracks += Track(trackSignature, trackLength, trackEvents)
//  }
//
//  return Midi(header = header, tracks = tracks)
//}
//
//fun main() {
//  val midi = parse("example3.mid")
//  println(midi)
//}