package com.daram.keyboard.input

import android.view.KeyEvent
import com.daram.keyboard.model.KeyAction

/**
 * KS X 5002 표준 두벌식 하드웨어 키보드 매퍼.
 * 나랏글·천지인 등 소프트 전용 자판에서도 하드웨어 입력 시 이 매퍼를 사용한다.
 *
 * 물리 Shift 키 상태는 event.isShiftPressed 로 판단하며,
 * 소프트 ShiftState는 사용하지 않는다.
 */
object KoreanDubeolsikHardwareKeyMapper : HardwareKeyMapper {

    // KS X 5002 두벌식 기본 배열 (Shift 없음)
    private val baseMap = mapOf(
        KeyEvent.KEYCODE_Q to 'ㅂ',
        KeyEvent.KEYCODE_W to 'ㅈ',
        KeyEvent.KEYCODE_E to 'ㄷ',
        KeyEvent.KEYCODE_R to 'ㄱ',
        KeyEvent.KEYCODE_T to 'ㅅ',
        KeyEvent.KEYCODE_Y to 'ㅛ',
        KeyEvent.KEYCODE_U to 'ㅕ',
        KeyEvent.KEYCODE_I to 'ㅑ',
        KeyEvent.KEYCODE_O to 'ㅐ',
        KeyEvent.KEYCODE_P to 'ㅔ',
        KeyEvent.KEYCODE_A to 'ㅁ',
        KeyEvent.KEYCODE_S to 'ㄴ',
        KeyEvent.KEYCODE_D to 'ㅇ',
        KeyEvent.KEYCODE_F to 'ㄹ',
        KeyEvent.KEYCODE_G to 'ㅎ',
        KeyEvent.KEYCODE_H to 'ㅗ',
        KeyEvent.KEYCODE_J to 'ㅓ',
        KeyEvent.KEYCODE_K to 'ㅏ',
        KeyEvent.KEYCODE_L to 'ㅣ',
        KeyEvent.KEYCODE_Z to 'ㅋ',
        KeyEvent.KEYCODE_X to 'ㅌ',
        KeyEvent.KEYCODE_C to 'ㅊ',
        KeyEvent.KEYCODE_V to 'ㅍ',
        KeyEvent.KEYCODE_B to 'ㅠ',
        KeyEvent.KEYCODE_N to 'ㅜ',
        KeyEvent.KEYCODE_M to 'ㅡ',
    )

    // KS X 5002 두벌식 Shift 배열 (쌍자음·이중모음)
    private val shiftMap = mapOf(
        KeyEvent.KEYCODE_Q to 'ㅃ',
        KeyEvent.KEYCODE_W to 'ㅉ',
        KeyEvent.KEYCODE_E to 'ㄸ',
        KeyEvent.KEYCODE_R to 'ㄲ',
        KeyEvent.KEYCODE_T to 'ㅆ',
        KeyEvent.KEYCODE_O to 'ㅒ',
        KeyEvent.KEYCODE_P to 'ㅖ',
    )

    override fun mapKeyCode(keyCode: Int, event: KeyEvent, shiftState: ShiftState): KeyAction? {
        val isShifted = event.isShiftPressed

        return when (keyCode) {
            KeyEvent.KEYCODE_DEL -> KeyAction.Backspace
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> KeyAction.Enter
            KeyEvent.KEYCODE_SPACE -> KeyAction.Space
            // 물리 Shift/CapsLock 키 자체는 무시
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_CAPS_LOCK -> null
            else -> {
                val jamo = if (isShifted) shiftMap[keyCode] ?: baseMap[keyCode]
                           else baseMap[keyCode]
                if (jamo != null) {
                    KeyAction.TypeRawKey(jamo)
                } else {
                    // 숫자·기호 등 한글 매핑이 없는 키는 유니코드 그대로
                    val unicode = event.getUnicodeChar(event.metaState)
                    if (unicode != 0) KeyAction.TypeText(unicode.toChar().toString()) else null
                }
            }
        }
    }
}
