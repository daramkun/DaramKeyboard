package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

/**
 * 천지인/SKY-II/모토로라처럼 3×3+기능키 구조를 가진 전화기 자판 레이아웃 빌더.
 * 각 레이아웃의 자모 레이블과 행4 롱프레스 액션만 다르고 구조는 동일하다.
 */
internal fun buildPhoneLayout(
    l1: String, l2: String, l3: String,         // 행1 자모 레이블
    l4: String, l5: String, l6: String,         // 행2 자모 레이블
    l7: String, l8: String, l9: String,         // 행3 자모 레이블
    lStar: String, lStarLong: KeyAction? = null, // 행4 * 키 레이블 및 롱프레스
    l0: String,    l0Long:    KeyAction? = null, // 행4 0 키 레이블 및 롱프레스
    lHash: String                               // 행4 # 키 레이블
): List<List<Key>> = listOf(
    // 행 1: 기호 | c1 | c2 | c3 | ⌫
    listOf(
        Key("sym",   "기호", null, KeyAction.SwitchToSymbol, style = KeyStyle.FUNCTION),
        Key("c1",    l1,    "1",  KeyAction.TypeRawKey('1'), KeyAction.TypeNumber(1)),
        Key("c2",    l2,    "2",  KeyAction.TypeRawKey('2'), KeyAction.TypeNumber(2)),
        Key("c3",    l3,    "3",  KeyAction.TypeRawKey('3'), KeyAction.TypeNumber(3)),
        Key("del",   "",    null, KeyAction.Backspace, style = KeyStyle.BACKSPACE)
    ),
    // 행 2: 한↔EN | c4 | c5 | c6 | Space (행2~3 걸침)
    listOf(
        Key("lang",  "⌨",   null, KeyAction.SwitchToNextIme, style = KeyStyle.FUNCTION),
        Key("c4",    l4,    "4",  KeyAction.TypeRawKey('4'), KeyAction.TypeNumber(4)),
        Key("c5",    l5,    "5",  KeyAction.TypeRawKey('5'), KeyAction.TypeNumber(5)),
        Key("c6",    l6,    "6",  KeyAction.TypeRawKey('6'), KeyAction.TypeNumber(6)),
        Key("space", " ",   null, KeyAction.Space, style = KeyStyle.SPACE, rowSpan = 2)
    ),
    // 행 3: 😊 | c7 | c8 | c9 | [space 패딩]
    listOf(
        Key("emoji",     "😊", null, KeyAction.ShowEmoji, style = KeyStyle.FUNCTION),
        Key("c7",        l7,  "7",  KeyAction.TypeRawKey('7'), KeyAction.TypeNumber(7)),
        Key("c8",        l8,  "8",  KeyAction.TypeRawKey('8'), KeyAction.TypeNumber(8)),
        Key("c9",        l9,  "9",  KeyAction.TypeRawKey('9'), KeyAction.TypeNumber(9)),
        Key("space_pad", "",  null, KeyAction.Space, visible = false)
    ),
    // 행 4: * | 0 | # | ↵
    listOf(
        Key("cstar", lStar, "*",   KeyAction.TypeRawKey('*'), lStarLong),
        Key("c0",    l0,    "0",   KeyAction.TypeRawKey('0'), l0Long),
        Key("chash", lHash, "#",   KeyAction.TypeRawKey('#')),
        Key("enter", "",    null,  KeyAction.Enter, style = KeyStyle.ENTER)
    )
)

/**
 * 천지인 자판 레이아웃.
 * 1=ㆍ, 2=ㅣ, 3=ㅡ (모음 획), 4~9=자음 사이클, *=ㅁ/ㅇ 사이클, 0=획 추가/재입력, #=쌍자음
 * CheonjiinAutomata에 TypeRawKey로 각 char를 전달한다.
 */
object CheonjiinLayout : KeyboardLayout {
    override val secondaryLabelAlignment = SecondaryLabelAlignment.BOTTOM_RIGHT
    override val rows = buildPhoneLayout(
        "ㆍ", "ㅣ", "ㅡ",
        "ㄱ", "ㄴ", "ㄷ",
        "ㅂ", "ㅅ", "ㅈ",
        lStar = "ㅁ", lStarLong = KeyAction.TypeNumber(0),
        l0 = "획",
        lHash = "쌍"
    )
}
