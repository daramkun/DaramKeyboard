package com.daram.keyboard.model

enum class KeyboardMode {
    NARATGUL,
    QWERTY,
    SYMBOL
}

data class KeyboardState(
    val mode: KeyboardMode = KeyboardMode.NARATGUL,
    val isShiftActive: Boolean = false,
    val isCapsLock: Boolean = false
)
