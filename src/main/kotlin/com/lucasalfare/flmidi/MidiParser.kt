@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader
import java.io.File


// não quero deixar valores mágicos hardcoded, quero que eles fiquem bem descritos
// contantes é a melhor abordagem? Enums seria melhor? Só quero que funcione...
const val META_EVENT = 0xFF
const val SYSTEM_EXCLUSIVE_EVENT = 0xF0
const val SYSTEM_EXCLUSIVE_ESCAPE_EVENT = 0xF7

const val META_EVENT_SEQUENCE_NUMBER = 0x00

open class Event

// esse é meu rascunho de MetaEvent mas não sei se tá com a melhor descrição possível
// e também ainda não defini os outros tipo de event
data class MetaEvent(
  val deltaTime: Int,
  val eventType: Int,
  val data: Any
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
    if (format == 0)
      require(numTracks == 1)
    else if (format == 1 || format == 2)
      require(numTracks >= 1)
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

// função principal, acho que funciona, só falta implementar cada coisa e também falta implementar o Running Status!
fun readMidi(pathname: String): Midi {
  val file = File(pathname)

  if (!file.exists()) error("file not exists")
  if (file.isDirectory) error("path [$pathname] is a directory, not a file")

  val fileBytes = file.readBytes().toUByteArray()
  val reader = Reader(fileBytes)

  val header = Header(
    chunkType = reader.readString(4) ?: error("no signature of header chunk type!"),
    length = reader.read4Bytes(),
    format = reader.read2Bytes(),
    numTracks = reader.read2Bytes(),
    division = reader.read2Bytes()
  )

  // minha ideia é ir coletando as trilhas
  val tracks = mutableListOf<Track>()

  repeat(header.numTracks) {
    val trackType = reader.readString(4) ?: error("no signature of track chunk type!")
    val trackLength = reader.read4Bytes().toInt()
    val finalOffset = reader.position + trackLength
    val events = mutableListOf<Event>()

    do {
      val currentDeltaTime = reader.readVariableLengthValue()
      val currentEvent = reader.read1Byte()

      // vou fazendo de acordo com o tipo de evento que nos deparamos
      when (currentEvent) {
        // meta-events
        META_EVENT -> {
          val currentMetaEventType = reader.read1Byte()

          // se for um MetaEvent, basicamente a gente deve tratar cada um, eu acho
          when (currentMetaEventType) {
            META_EVENT_SEQUENCE_NUMBER -> {
              // exeemplo: events += MetaEvent(currentDeltaTime, META_EVENT_SEQUENCE_NUMBER, listOf(0x02))
            }

            else -> {

            }
          }
        }

        // sys-ex events
        SYSTEM_EXCLUSIVE_EVENT, SYSTEM_EXCLUSIVE_ESCAPE_EVENT -> {
          // TODO: não me importo com esses eventos, posso pular eles, mas de forma adequada!
        }

        // MIDI events/Control Events
        else -> {
          // TODO: falta tudo aqui também!
        }
      }
    } while (reader.position < finalOffset)

    tracks += Track(
      type = trackType,
      length = trackLength,
      events = events
    )
  }

  return Midi(header = header, tracks = tracks)
}

fun main() {
  println(readMidi("example2.mid"))
}