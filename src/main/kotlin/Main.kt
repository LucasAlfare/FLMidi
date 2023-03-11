import com.lucasalfare.flbinary.Reader
import java.io.File

@ExperimentalUnsignedTypes
fun main() {
  val f = File("test.mid")
  if (f.exists() && !f.isDirectory) {
    val reader = Reader(f.readBytes().toUByteArray())

    val headerSignature = reader.readString(4)
    val headerLength = reader.read4Bytes()
    val midiFormat = reader.read2Bytes()
    val numberOfTracks = reader.read2Bytes()
    val timeDivision = reader.read2Bytes()

    println("headerSignature=$headerSignature")
    println("headerLength=$headerLength")
    println("midiFormat=$midiFormat")
    println("numberOfTracks=$numberOfTracks")
    println("timeDivision=$timeDivision")

    println()

    for (j in 0..numberOfTracks) {
      val trackChunkSignature = reader.readString(4)
      val trackChunkBytesSize = reader.read4Bytes()
      println("trackChunkSignature=$trackChunkSignature")
      println("trackChunkBytesSize=$trackChunkBytesSize")
      println()

      for (i in 0..trackChunkBytesSize) {
        parseDeltaTime(reader)

        println(Integer.toHexString(reader.read2Bytes()))
        // TODO: parse the event here...
        break
      }

      break
    }
  }
}

fun parseDeltaTime(reader: Reader) {
  // parse delta time
  var deltaTime = 0x00
  var deltaParsed = false
  val auxMask = 0b0111_1111
  val tmpShiftedBytes = mutableListOf<Int>()

  while (!deltaParsed) {
    val tmpByte = reader.read1Byte()
    val seventhBit = tmpByte ushr 7

    tmpShiftedBytes += tmpByte and auxMask

    if (seventhBit == 0) {
      tmpShiftedBytes.forEach { b ->
        deltaTime = (deltaTime shl 7) or b
      }
      println("The final delta is ${Integer.toHexString(deltaTime)}")
      deltaParsed = true
    }
  }
}
