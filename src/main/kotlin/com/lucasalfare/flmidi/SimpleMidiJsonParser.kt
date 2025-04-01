package com.lucasalfare.flmidi

import kotlinx.serialization.json.Json

object SimpleMidiJsonParser {

  private var json = Json { prettyPrint = false }
  private var prettyPrintJson = Json { prettyPrint = true }

  fun midiToJson(midi: Midi, formatted: Boolean = false): String {
    if (formatted) return prettyPrintJson.encodeToString(midi)
    return json.encodeToString(midi)
  }

  fun jsonToMidi(jsonString: String): Midi {
    return json.decodeFromString<Midi>(jsonString)
  }
}