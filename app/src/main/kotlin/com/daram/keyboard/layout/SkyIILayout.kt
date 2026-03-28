package com.daram.keyboard.layout

/**
 * SKY-II 자판 레이아웃.
 * 멀티탭 순환 방식. SkyIIAutomata에 TypeRawKey로 char를 전달한다.
 * 1=모음계열, 2~9=자음계열, 0=획추가/순환, *=ㅇ/ㅎ계열, #=쌍자음
 */
object SkyIILayout : KeyboardLayout {
    override val secondaryLabelAlignment = SecondaryLabelAlignment.BOTTOM_RIGHT
    override val rows = buildPhoneLayout(
        "ㅣ…", "ㄱ…", "ㄴ…",
        "ㄷ…", "ㄹ",  "ㅁ",
        "ㅂ…", "ㅅ…", "ㅈ…",
        lStar = "ㅇ…",
        l0 = "획", l0Long = com.daram.keyboard.model.KeyAction.TypeNumber(0),
        lHash = "쌍"
    )
}
