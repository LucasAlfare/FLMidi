package com.lucasalfare.flmidi

fun Int.toHexString() = "0x${Integer.toHexString(this).padStart(2, '0')}"

// hardcoded padding only to 4 units
fun Int.toBinaryString() = "0b${Integer.toBinaryString(this).padStart(4, '0')}"
