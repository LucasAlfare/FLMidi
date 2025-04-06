package hehe

import com.lucasalfare.flmidi.MetaEvent
import com.lucasalfare.flmidi.Track
import com.lucasalfare.flmidi.readMidi

fun main() {
  val midi = readMidi("example3.mid")

  midi.tracks.forEachIndexed { index: Int, track: Track ->
    track.events.filterIsInstance<MetaEvent>().forEach {
      println("MetaEvent in track $index: $it")
    }
  }

//  println(SimpleMidiJsonParser.midiToJson(midi))
}