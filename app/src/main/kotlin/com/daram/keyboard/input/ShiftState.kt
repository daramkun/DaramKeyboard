package com.daram.keyboard.input

enum class ShiftState { OFF, ONCE, LOCKED }

fun ShiftState.next(): ShiftState = when (this) {
    ShiftState.OFF    -> ShiftState.ONCE
    ShiftState.ONCE   -> ShiftState.LOCKED
    ShiftState.LOCKED -> ShiftState.OFF
}
