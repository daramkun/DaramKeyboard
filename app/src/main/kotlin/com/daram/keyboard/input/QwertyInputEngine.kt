package com.daram.keyboard.input

import android.view.inputmethod.InputConnection
import com.daram.keyboard.model.KeyAction

class QwertyInputEngine : InputEngine {

    private var shiftState = ShiftState.OFF

    override fun processAction(action: KeyAction, inputConnection: InputConnection) {
        when (action) {
            is KeyAction.TypeText -> {
                val text = if (shiftState != ShiftState.OFF) action.text.uppercase() else action.text
                inputConnection.commitText(text, 1)
                if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
            }
            is KeyAction.TypeNumber -> {
                inputConnection.commitText(action.digit.toString(), 1)
            }
            is KeyAction.Backspace -> {
                inputConnection.deleteSurroundingText(1, 0)
            }
            is KeyAction.Space -> {
                inputConnection.commitText(" ", 1)
                if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
            }
            is KeyAction.Enter -> {
                inputConnection.commitText("\n", 1)
            }
            is KeyAction.Shift -> {
                shiftState = shiftState.next()
            }
            else -> { /* 나머지 액션은 서비스에서 처리 */ }
        }
    }

    fun getShiftState(): ShiftState = shiftState

    override fun reset() {
        shiftState = ShiftState.OFF
    }
}
