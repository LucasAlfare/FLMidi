@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("MoveVariableDeclarationIntoWhen")

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File


fun loadAndReadMidiFile(pathname: String = ""): MidiInfo {
  val file = File(pathname)
  if (file.exists() && !file.isDirectory) {
    val fileData = file.readBytes()
    val reader = Reader(fileData.toUByteArray())

    val header = Header()
    header.signature = reader.readString(4)!!
    header.length = reader.read4Bytes()
    header.midiFormat = reader.read2Bytes()
    header.numTracks = reader.read2Bytes()
    header.timeDivision = reader.read2Bytes()

    val tracks = mutableListOf<Track>()

    repeat(header.numTracks) {
      val currentTrack = Track()
      currentTrack.signature = reader.readString(4)!!
      currentTrack.length = reader.read4Bytes()

      for (i in 0..currentTrack.length) {
        val currentDeltaTime = readVariableLength(reader)
        val currentStatusByte = reader.read1Byte()

        when (currentStatusByte) {
          0xff -> { // meta event
            val currentEventTypeByte = reader.read1Byte()
            val event = MetaEvent()
            event.category = EventCategory.Meta
            event.deltaTime = currentDeltaTime
            event.type = getMetaEventTypeByCode(currentEventTypeByte)
            event.defineData(reader)

            currentTrack.events += event

            if (event.type == MetaEventType.EndOfTrack) {
              break
            }
          }

          0xf0, 0xf7 -> {
            // sysex event
          }

          else -> {
            // control event
            val event = ControlEvent()
            event.category = EventCategory.Control
            event.deltaTime = currentDeltaTime
            event.type = getControlEventTypeByCode(currentStatusByte)
            event.targetChannel = currentStatusByte and 0b1111
            event.defineData(reader)

            currentTrack.events += event
          }
        }
      }

      tracks += currentTrack
    }

    return MidiInfo(
      header = header,
      tracks = tracks
    )
  }

  return MidiInfo()
}

fun exampleToBeRun() {
  val info = loadAndReadMidiFile("WelcomeToBucketheadland.mid")
  info.tracks.first().events.forEach {
    if (it.category == EventCategory.Meta) {
      println(it)
    }
  }
}

fun main() {
  exampleToBeRun()
}