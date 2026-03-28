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
open class HangulInputEngine(
    private val automata: HangulAutomata,
    val hasShift: Boolean = false
) : InputEngine {

    var shiftState: ShiftState = ShiftState.OFF
        protected set

    /** 자소/음절이 commit될 때 외부에서 추적하는 콜백 */
    var onTextCommitted: ((String) -> Unit)? = null

    /** 마지막으로 입력된 자소 (히트 타겟 조정용). 서브클래스에서 오버라이드 가능 */
    open val lastInputJamo: Char? get() = null

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
            }
            is KeyAction.TypeHangul -> processInput(KeyInput.Char(action.jamo), inputConnection)
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
                val flushed = flushAndCommit(inputConnection)
                if (flushed.isNotEmpty()) onTextCommitted?.invoke(flushed)
                inputConnection.commitText(" ", 1)
            }
            is KeyAction.Enter -> {
                val flushed = flushAndCommit(inputConnection)
                if (flushed.isNotEmpty()) onTextCommitted?.invoke(flushed)
                inputConnection.commitText("\n", 1)
            }
            is KeyAction.TypeText -> {
                val flushed = flushAndCommit(inputConnection)
                inputConnection.commitText(action.text, 1)
                onTextCommitted?.invoke(flushed + action.text)
            }
            is KeyAction.TypeNumber -> {
                val flushed = flushAndCommit(inputConnection)
                inputConnection.commitText(action.digit.toString(), 1)
                onTextCommitted?.invoke(flushed + action.digit.toString())
            }
            is KeyAction.Shift -> {
                if (hasShift) shiftState = shiftState.next()
            }
            else -> {}
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
    }
}
