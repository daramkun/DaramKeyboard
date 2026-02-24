package com.daram.keyboard.model

enum class KeyStyle {
    NORMAL,
    FUNCTION,
    SPACE,
    ENTER,
    SHIFT,
    BACKSPACE,
    /** 이전 레이아웃 복귀 버튼 — 렌더링 시 prevLayout에 따라 레이블 결정 */
    RETURN
}

data class Key(
    val id: String,
    val primaryLabel: String,
    val secondaryLabel: String? = null,
    val action: KeyAction,
    val longPressAction: KeyAction? = null,
    val widthWeight: Float = 1f,
    val rowSpan: Int = 1,
    val style: KeyStyle = KeyStyle.NORMAL,
    /** false이면 렌더링 및 터치 처리에서 제외되는 투명 여백 키 */
    val visible: Boolean = true
)
