package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File
import kotlin.math.pow

open class MyEvent

data class TrackMetaEvent(
  val type: Int,
  val delta: Int,
  val data: Any? = null
) : MyEvent() {
  override fun toString() = "TrackMetaEvent(type=${type.toHexString()}, delta=$delta, data=$data)"
}

data class TrackControlEvent(
  val type: Int,
  val delta: Int,
  val data: Any? = null,
  val targetChannel: Int = type and 0b1111
) : MyEvent() {
  override fun toString() =
    "TrackControlEvent(type=${type.toBinaryString()}, delta=$delta, data=$data, targetChannel=$targetChannel)"
}

data class MyTrack(
  val signature: String,
  val length: Long,
  val events: MutableList<MyEvent>
) {
  override fun toString() = buildString {
    append("\tSignature: $signature\n")
    append("\tLength: $length\n")
    events.forEach { append("\t$it"); append("\n") }
  }
}

data class MyHeader(
  val signature: String,
  val length: Long,
  val midiFormat: Int,
  val numTracks: Int,
  val timeDivision: Int
)

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
  val file = File("midi_format_1_example.mid")
  if (file.exists() && !file.isDirectory) {
    val reader = Reader(file.readBytes().toUByteArray())

    val myHeader = MyHeader(
      signature = reader.readString(4) ?: "no signature",
      length = reader.read4Bytes(),
      midiFormat = reader.read2Bytes(),
      numTracks = reader.read2Bytes(),
      timeDivision = reader.read2Bytes()
    )

    println(myHeader)

    println("~Warning: this is a midi file in format ${myHeader.midiFormat}!~")

    val tracks = mutableListOf<MyTrack>()

    for (i in 0..<myHeader.numTracks) {
      val trackSignature = reader.readString(4)
      val trackLength = reader.read4Bytes()
      var previousStatus = 0
      val trackEvents = mutableListOf<MyEvent>()
      while (true) {
        val delta = reader.readVariableLengthValue()
        var currentStatus = reader.read1Byte()

        if (currentStatus ushr 7 == 0) {
          currentStatus = previousStatus
          reader.position--
        }

        when (currentStatus) {
          0xFF -> {
            when (val type = reader.read1Byte()) {
              // text event, copyright notice, track name, instrument name, lyric, marker, cue point
              0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 -> {
                val textLength = reader.readVariableLengthValue()
                val data = reader.readString(textLength)!!
                trackEvents += TrackMetaEvent(type, delta, data)
              }

              // time signature
              0x58 -> {
                val numDataItems = reader.read1Byte()

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

                trackEvents += TrackMetaEvent(type, delta, data)
              }

              // set tempo in microseconds per quarter note
              0x51 -> {
                val numDataItems = reader.read1Byte()
                val tempoInMicroseconds = reader.read3Bytes()
                trackEvents += TrackMetaEvent(type, delta, tempoInMicroseconds)
              }

              // SMPTE Offset
              0x54 -> {
                val dataLength = reader.read1Byte()
                val data = listOf(
                  reader.read1Byte(),
                  reader.read1Byte(),
                  reader.read1Byte(),
                  reader.read1Byte(),
                  reader.read1Byte()
                )

                trackEvents += TrackMetaEvent(type, delta, data)
              }

              // key signature
              0x59 -> {
                val dataLength = reader.read1Byte()
                val data = listOf(reader.read1Byte(), reader.read1Byte())
                trackEvents += TrackMetaEvent(type, delta, data)
              }


              // midi channel prefix
              0x20 -> {
                val dataLength = reader.read1Byte()
                val currentEffectiveMidiChannel = reader.read1Byte()
                trackEvents += TrackMetaEvent(type, delta, currentEffectiveMidiChannel)
              }

              // sequencer specific meta event
              0x7f -> {
                val dataLength = reader.readVariableLengthValue()
                val auxBytes = mutableListOf<Int>()
                repeat(dataLength) {
                  auxBytes += reader.read1Byte()
                }

                trackEvents += TrackMetaEvent(type, delta, auxBytes)
              }

              // end of track
              0x2F -> {
                trackEvents += TrackMetaEvent(type, delta)
                break
              }

              else -> {
                error("Unknown track meta event type: ${type.toHexString()}")
              }
            }
          }

          0xF0, 0xF7 -> {
            // pass...
          }

          else -> {
            // updates running status "mode"
            previousStatus = currentStatus

            when (val type = currentStatus shr 4) {
              // select instrument
              0b1100 -> {
                val targetInstrument = reader.read1Byte()
                trackEvents += TrackControlEvent(type, delta, targetInstrument)
              }

              // note on, note off
              0b1001, 0b1000 -> {
                val noteNumber = reader.read1Byte() and 0b01111111
                val noteVelocity = reader.read1Byte() and 0b01111111
                val data = listOf(noteNumber, noteVelocity)
                trackEvents += TrackControlEvent(type, delta, data)
              }

              // channel mode
              0b1011 -> {
                val channelModeTypeCode = reader.read1Byte()
                val channelModeArg1 = reader.read1Byte()
                val channelModeArg2 = reader.read1Byte()
                val data = listOf(
                  channelModeTypeCode,
                  channelModeArg1,
                  channelModeArg2
                )
                trackEvents += TrackControlEvent(type, delta, data)
              }

              // channel pressure
              0b1101 -> {
                val channelPressure = reader.read1Byte()
                trackEvents += TrackControlEvent(type, delta, channelPressure)
              }

              // pitch bend
              0b1110 -> {
                val pitchBend = reader.read1Byte()
                trackEvents += TrackControlEvent(type, delta, pitchBend)
              }

              else -> {
                error("Unknown track control event type: ${type.toHexString()}")
              }
            }
          }
        }
      }
      tracks += MyTrack(trackSignature ?: "no signature", trackLength, trackEvents)

      break
    }

    tracks.forEachIndexed { index, myTrack ->
      println("Track ${index + 1}:")
      println(myTrack)
    }
  }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun specFileFormat1() {
  val s =
    "4D 54 68 64 00 00 00 06 00 01 00 04 00 60 4D 54 72 6B 00 00 00 14 FF 58 04 04 02 18 08 FF 51 03 07 A1 20 FF 2F 00 4D 54 72 6B 00 00 00 10 C0 05 90 4C 20 4C 00 FF 2F 00 4D 54 72 6B 00 00 00 0F C1 2E 91 43 40 43 00 FF 2F 00 4D 54 72 6B 00 00 00 15 C2 46 92 30 60 3C 60 30 00 3C 00 FF 2F 00"
      .replace("\t", "")
      .replace("\n", " ")
      .replace("  ", " ")
      .trim()
      .split(" ")
  println(s)

  val bytes = UByteArray(s.size)
  s.forEachIndexed { index, b ->
    if (b.isNotEmpty()) {
      bytes[index] = Integer.parseInt(b, 16).toUByte()
    }
  }

  val f = File("midi_format_1_example.mid")
  f.writeBytes(bytes.toByteArray())
}