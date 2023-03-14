package com.lucasalfare.flmidi

import com.lucasalfare.flbinary.Reader

data class Header(
  var signature: String = "",
  var length: Long = 0,
  var midiFormat: Int = 0,
  var numTracks: Int = 0,
  var timeDivision: Int = 0
) {
  fun define(reader: Reader) {
    signature = reader.readString(4)!!
    length = reader.read4Bytes()
    midiFormat = reader.read2Bytes()
    numTracks = reader.read2Bytes()
    timeDivision = reader.read2Bytes()
  }
}