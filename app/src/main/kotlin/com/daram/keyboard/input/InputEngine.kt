package com.daram.keyboard.input

import android.view.inputmethod.InputConnection
import com.daram.keyboard.model.KeyAction

interface InputEngine {
    fun processAction(action: KeyAction, inputConnection: InputConnection)
    fun reset()
}
