package com.daram.keyboard.input

import android.view.inputmethod.InputConnection
import com.daram.keyboard.model.KeyAction

class NaratgulInputEngine : InputEngine {
    val composer = HangulComposer()

    /** commit되는 텍스트(확정 글자)를 외부에서 추적할 때 사용 */
    var onTextCommitted: ((String) -> Unit)? = null

    /**
     * 마지막으로 실제 입력된 자소.
     * TypeHangul 처리 후 자소 단위 히트 타겟 조정에 사용하기 위해 노출.
     * 한글 외 액션(백스페이스, 스페이스 등)에서는 null로 초기화됨.
     */
    var lastInputJamo: Char? = null
        private set

    override fun processAction(action: KeyAction, inputConnection: InputConnection) {
        when (action) {
            is KeyAction.TypeHangul -> {
                val result = composer.inputJamo(action.jamo)
                // 실제로 조합에 반영된 자소를 추적
                // resolveVowelCycle 이후의 실제 자소는 composer가 내부 처리하므로,
                // 현재 조합 텍스트의 마지막 자소(자모 분해)를 통해 확인
                lastInputJamo = resolveLastJamo(result)
                if (result.committed.isNotEmpty()) {
                    // commitText가 composing 텍스트를 자동으로 교체하므로
                    // finishComposingText()를 먼저 호출하면 안 됨
                    inputConnection.commitText(result.committed, 1)
                    onTextCommitted?.invoke(result.committed)
                }
                if (result.composing.isNotEmpty()) {
                    inputConnection.setComposingText(result.composing, 1)
                } else {
                    inputConnection.finishComposingText()
                }
            }
            is KeyAction.TypeText -> {
                lastInputJamo = null
                val flushed = commitAndFlush(inputConnection)
                inputConnection.commitText(action.text, 1)
                onTextCommitted?.invoke(flushed + action.text)
            }
            is KeyAction.TypeNumber -> {
                lastInputJamo = null
                val flushed = commitAndFlush(inputConnection)
                inputConnection.commitText(action.digit.toString(), 1)
                onTextCommitted?.invoke(flushed + action.digit.toString())
            }
            is KeyAction.Backspace -> {
                lastInputJamo = null
                if (!composer.isEmpty()) {
                    val composing = composer.backspace()
                    if (composing.isNotEmpty()) {
                        inputConnection.setComposingText(composing, 1)
                    } else {
                        inputConnection.finishComposingText()
                    }
                } else {
                    inputConnection.deleteSurroundingText(1, 0)
                }
            }
            is KeyAction.Space -> {
                lastInputJamo = null
                val flushed = commitAndFlush(inputConnection)
                if (flushed.isNotEmpty()) onTextCommitted?.invoke(flushed)
                inputConnection.commitText(" ", 1)
            }
            is KeyAction.Enter -> {
                lastInputJamo = null
                val flushed = commitAndFlush(inputConnection)
                if (flushed.isNotEmpty()) onTextCommitted?.invoke(flushed)
                inputConnection.commitText("\n", 1)
            }
            is KeyAction.AddStroke -> {
                lastInputJamo = null
                if (composer.addStroke()) {
                    inputConnection.setComposingText(composer.getComposingText(), 1)
                }
            }
            is KeyAction.ToggleDouble -> {
                lastInputJamo = null
                if (composer.toggleDouble()) {
                    inputConnection.setComposingText(composer.getComposingText(), 1)
                }
            }
            else -> { }
        }
    }

    /**
     * 입력 결과에서 현재 조합 중인 자소를 추출 (히트 타겟 추적용).
     *
     * composing 텍스트에서만 추출한다. committed만 있고 composing이 없는 경우
     * (음절 확정 후 새 조합 없음)는 null 반환 — 이미 확정된 글자로 히트 타겟을
     * 조정하는 것은 의미가 없기 때문.
     */
    private fun resolveLastJamo(result: HangulComposer.InputResult): Char? {
        if (result.composing.isEmpty()) return null
        return decomposeLastJamo(result.composing.last())
    }

    /**
     * 한글 문자에서 현재 조합의 마지막 자소를 분해하여 반환.
     * 완성형 음절: 종성(있으면) > 중성 순으로 가장 나중에 입력된 자소.
     * 낱자: 그대로 반환.
     * 한글이 아니면 null.
     */
    private fun decomposeLastJamo(c: Char): Char? {
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

    /** 조합 중인 텍스트 확정, 확정된 문자열 반환 */
    private fun commitAndFlush(inputConnection: InputConnection): String {
        val flushed = composer.flushComposing()
        if (flushed.isNotEmpty()) {
            inputConnection.commitText(flushed, 1)
        } else {
            inputConnection.finishComposingText()
        }
        return flushed
    }

    override fun reset() {
        composer.reset()
    }
}
