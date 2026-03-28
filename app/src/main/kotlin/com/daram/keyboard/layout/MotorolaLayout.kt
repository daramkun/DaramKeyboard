package com.daram.keyboard.layout

/**
 * 모토로라 한글 자판 레이아웃.
 * MotorolaAutomata에 TypeRawKey로 char를 전달한다.
 * 1~9: 자음/모음 사이클, 0: 모음 획, *: 특수, #: 모드 전환
 */
object MotorolaLayout : KeyboardLayout {
    override val secondaryLabelAlignment = SecondaryLabelAlignment.BOTTOM_RIGHT
    override val rows = buildPhoneLayout(
        "ㆍ", "ㅣ", "ㅡ",
        "ㄱ", "ㄴ", "ㄷ",
        "ㅂ", "ㅅ", "ㅈ",
        lStar = "획+",
        l0 = "0", l0Long = com.daram.keyboard.model.KeyAction.TypeNumber(0),
        lHash = "모드"
    )
}
