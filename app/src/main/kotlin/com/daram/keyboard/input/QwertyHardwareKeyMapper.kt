package com.daram.keyboard.input

import android.view.KeyEvent
import com.daram.keyboard.model.KeyAction

/**
 * 영문 QWERTY 하드웨어 키보드 매퍼.
 * 물리 Shift/CapsLock 상태는 event.metaState를 통해 처리되므로
 * 소프트 ShiftState를 별도로 적용하지 않는다.
 */
object QwertyHardwareKeyMapper : HardwareKeyMapper {
    override fun mapKeyCode(keyCode: Int, event: KeyEvent, shiftState: ShiftState): KeyAction? {
        return when (keyCode) {
            KeyEvent.KEYCODE_DEL -> KeyAction.Backspace
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> KeyAction.Enter
            KeyEvent.KEYCODE_SPACE -> KeyAction.Space
            // 물리 Shift/CapsLock 키 자체는 metaState로 처리되므로 무시
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_CAPS_LOCK -> null
            else -> {
                val unicode = event.getUnicodeChar(event.metaState)
                if (unicode != 0) KeyAction.TypeText(unicode.toChar().toString()) else null
            }
        }
    }
}
