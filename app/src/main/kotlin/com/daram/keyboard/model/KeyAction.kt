package com.daram.keyboard.model

sealed class KeyAction {
    data class TypeText(val text: String) : KeyAction()
    data class TypeHangul(val jamo: Char) : KeyAction()
    object Backspace : KeyAction()
    object Enter : KeyAction()
    object Space : KeyAction()
    object SwitchToQwerty : KeyAction()
    object SwitchToNaratgul : KeyAction()
    object SwitchToSymbol : KeyAction()
    /** 나랏글 숫자 레이아웃으로 전환 */
    object SwitchToNumber : KeyAction()
    /** 기호 레이아웃 다음 페이지 */
    object SymbolNextPage : KeyAction()
    /** 이모지 레이아웃으로 전환 */
    object ShowEmoji : KeyAction()
    /** 이모지 카테고리 페이지 전환 */
    data class SwitchEmojiCategory(val categoryIndex: Int) : KeyAction()
    object AddStroke : KeyAction()
    object ToggleDouble : KeyAction()
    object Shift : KeyAction()
    /** Shift 고정 (CapsLock) */
    object ShiftLock : KeyAction()
    data class TypeNumber(val digit: Int) : KeyAction()
    /** QWERTY 전용 기호 레이아웃으로 전환 */
    object SwitchToQwertySymbol : KeyAction()
    /** 이전 레이아웃(나랏글 또는 QWERTY)으로 복귀 */
    object ReturnToPrev : KeyAction()
}
