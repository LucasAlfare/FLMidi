package com.lucasalfare.flmidi

fun Int.toHexString() = Integer.toHexString(this).padStart(2, '0')

fun Int.toBinaryString() = Integer.toBinaryString(this).padStart(8, '0')