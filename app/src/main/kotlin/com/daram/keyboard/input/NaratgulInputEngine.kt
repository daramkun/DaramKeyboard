package com.daram.keyboard.input

import android.view.inputmethod.InputConnection
import com.daram.keyboard.model.KeyAction
import `in`.daram.nutcracker.KeyInput
import `in`.daram.nutcracker.toComposingString
import `in`.daram.nutcracker.automata.NaratgeulAutomata

class NaratgulInputEngine : HangulInputEngine(NaratgeulAutomata()) {

    private var _lastInputJamo: Char? = null

    /**
     * 마지막으로 실제 입력된 자소 (히트 타겟 조정용).
     * TypeHangul 처리 후 조합 텍스트의 마지막 자소를 반영.
     * 한글 외 액션에서는 null.
     */
    override val lastInputJamo: Char? get() = _lastInputJamo

    override fun processAction(action: KeyAction, inputConnection: InputConnection) {
        when (action) {
            is KeyAction.TypeHangul -> {
                super.processAction(action, inputConnection)
                _lastInputJamo = decomposeLastJamo(syllableState.toComposingString().lastOrNull())
            }
            else -> {
                _lastInputJamo = null
                super.processAction(action, inputConnection)
            }
        }
    }

    override fun reset() {
        super.reset()
        _lastInputJamo = null
    }

    /**
     * 한글 문자에서 현재 조합의 마지막 자소를 분해하여 반환.
     * 완성형 음절: 종성(있으면) > 중성 순.
     * 낱자: 그대로 반환. 한글이 아니면 null.
     */
    private fun decomposeLastJamo(c: Char?): Char? {
        c ?: return null
        val code = c.code
        return when {
            code in 0xAC00..0xD7A3 -> {
                val offset = code - 0xAC00
                val jongIndex = offset % 28
                val jungIndex = (offset / 28) % 21
                val jongseongList = charArrayOf(
                    '\u0000','ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ',
                    'ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ',
                    'ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
                )
                val jungseongList = charArrayOf(
                    'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ',
                    'ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ'
                )
                if (jongIndex != 0) jongseongList[jongIndex] else jungseongList[jungIndex]
            }
            code in 0x3131..0x3163 -> c
            else -> null
        }
    }
}
