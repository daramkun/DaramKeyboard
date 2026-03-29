package com.daram.keyboard.input

import android.view.inputmethod.InputConnection
import com.daram.keyboard.model.KeyAction
import `in`.daram.nutcracker.KeyInput
import `in`.daram.nutcracker.SpecialKey
import `in`.daram.nutcracker.SyllableState
import `in`.daram.nutcracker.toComposingString
import `in`.daram.nutcracker.HangulAutomata

/**
 * 모든 한글 입력 엔진의 공통 기반 클래스.
 * nutcracker의 HangulAutomata 구현체를 감싸고,
 * TypeRawKey / TypeHangul / AddStroke / ToggleDouble / Backspace / Space / Enter 등을 처리한다.
 *
 * @param automata  nutcracker HangulAutomata 구현체 (NaratgeulAutomata, CheonjiinAutomata 등)
 * @param hasShift  두벌식·단모음처럼 Shift 키가 쌍자음 전환에 필요한 경우 true
 */
class HangulInputEngine(
    private val automata: HangulAutomata,
    val hasShift: Boolean = false
) : InputEngine {

    var shiftState: ShiftState = ShiftState.OFF

    /** 자소/음절이 commit될 때 외부에서 추적하는 콜백 */
    var onTextCommitted: ((String) -> Unit)? = null

    private var _lastInputJamo: Char? = null

    /** 마지막으로 입력된 자소 (히트 타겟 조정용) */
    val lastInputJamo: Char? get() = _lastInputJamo

    private var state: SyllableState = automata.initialState()

    val syllableState: SyllableState get() = state

    fun getComposingText(): String = state.toComposingString()
    fun isEmpty(): Boolean = getComposingText().isEmpty()

    override fun processAction(action: KeyAction, inputConnection: InputConnection) {
        when (action) {
            is KeyAction.TypeRawKey -> {
                val ch = if (hasShift && shiftState != ShiftState.OFF)
                    action.char.uppercaseChar() else action.char
                processInput(KeyInput.Char(ch), inputConnection)
                if (hasShift && shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
                _lastInputJamo = decomposeLastJamo(state.toComposingString().lastOrNull())
            }
            is KeyAction.TypeHangul -> {
                processInput(KeyInput.Char(action.jamo), inputConnection)
                _lastInputJamo = decomposeLastJamo(state.toComposingString().lastOrNull())
            }
            is KeyAction.AddStroke  -> processInput(KeyInput.Char('*'), inputConnection)
            is KeyAction.ToggleDouble -> processInput(KeyInput.Char('#'), inputConnection)
            is KeyAction.Backspace -> {
                if (!isEmpty()) {
                    val result = automata.process(state, KeyInput.Special(SpecialKey.BACKSPACE))
                    state = result.newState
                    val composing = state.toComposingString()
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
                _lastInputJamo = null
                val flushed = flushAndCommit(inputConnection)
                if (flushed.isNotEmpty()) onTextCommitted?.invoke(flushed)
                inputConnection.commitText(" ", 1)
            }
            is KeyAction.Enter -> {
                _lastInputJamo = null
                val flushed = flushAndCommit(inputConnection)
                if (flushed.isNotEmpty()) onTextCommitted?.invoke(flushed)
                inputConnection.commitText("\n", 1)
            }
            is KeyAction.TypeText -> {
                _lastInputJamo = null
                val flushed = flushAndCommit(inputConnection)
                inputConnection.commitText(action.text, 1)
                onTextCommitted?.invoke(flushed + action.text)
            }
            is KeyAction.TypeNumber -> {
                _lastInputJamo = null
                val flushed = flushAndCommit(inputConnection)
                inputConnection.commitText(action.digit.toString(), 1)
                onTextCommitted?.invoke(flushed + action.digit.toString())
            }
            is KeyAction.Shift -> {
                if (hasShift) shiftState = shiftState.next()
            }
            else -> { _lastInputJamo = null }
        }
    }

    protected fun processInput(input: KeyInput, ic: InputConnection) {
        val result = automata.process(state, input)
        state = result.newState
        if (result.committed.isNotEmpty()) {
            if (result.committed == "\b") {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText(result.committed, 1)
                onTextCommitted?.invoke(result.committed)
            }
        }
        val composing = state.toComposingString()
        if (composing.isNotEmpty()) {
            ic.setComposingText(composing, 1)
        } else {
            ic.finishComposingText()
        }
    }

    protected fun flushAndCommit(ic: InputConnection): String {
        val result = automata.flush(state)
        state = automata.initialState()
        return if (result.committed.isNotEmpty() && result.committed != "\b") {
            ic.commitText(result.committed, 1)
            result.committed
        } else {
            ic.finishComposingText()
            ""
        }
    }

    override fun reset() {
        state = automata.initialState()
        shiftState = ShiftState.OFF
        _lastInputJamo = null
    }

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
