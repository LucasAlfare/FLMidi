import com.lucasalfare.flmidi.MidiReader
import com.lucasalfare.flmidi.MidiWriter

class Using {

  private fun usageDemo() {
    val inputFile = "input.mid"
    val outputFile = "output.mid"

    // Read the existing MIDI file.
    val midi = MidiReader.fromFile(inputFile)

    // Write the parsed MIDI model to a new MIDI file.
    MidiWriter.writeToFile(
      midi = midi,
      path = outputFile,
      useRunningStatus = true
    )

    println("MIDI successfully read from: $inputFile")
    println("MIDI successfully written to: $outputFile")
    println("If you listen to both files, both are the same!")
  }
}