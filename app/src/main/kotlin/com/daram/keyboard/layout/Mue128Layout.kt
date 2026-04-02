package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

/**
 * 무이128 자판 레이아웃 (§8-2 기준).
 * Mue128Automata에 TypeHangul로 한글 자모를 전달한다.
 * 동일 키 두 번 입력으로 쌍자음·이중모음을 생성한다.
 *
 *   행 1 (6키+공백): [ㅈ][ㄷ][ㄱ][ㅅ][ㅔ][ㅐ][ ]
 *   행 2 (7키):      [ㅁ][ㄴ][ㅇ][ㄹ][ㅓ][ㅏ][ㅣ]
 *   행 3 (⇧+7키+⌫): [⇧][ㅋ][ㅌ][ㅂ][ㅎ][ㅜ][ㅗ][ㅡ][⌫]
 *
 *   자음 멀티탭: ㅈ(ㅉ), ㄷ(ㄸ), ㄱ(ㄲ), ㅅ(ㅆ), ㅌ(ㅊ), ㅂ(ㅃ), ㅎ(ㅍ), ㄴ, ㄹ, ㅁ, ㅇ, ㅋ
 *   모음 멀티탭: ㅔ(ㅖ), ㅐ(ㅒ), ㅓ(ㅕ), ㅏ(ㅑ), ㅣ, ㅜ(ㅠ), ㅗ(ㅛ), ㅡ
 */
object Mue128Layout : KeyboardLayout {

    override val secondaryLabelAlignment = SecondaryLabelAlignment.BOTTOM_RIGHT

    override val rows: List<List<Key>> = listOf(
        // 행 1: ㅈ(1) ㄷ(2) ㄱ(3) ㅅ(4) ㅔ(5) ㅐ(6) [invisible] — 6키 연속, 맨 오른쪽 공백
        listOf(
            Key("m_j",   "ㅈ", "1", KeyAction.TypeHangul('ㅈ'), KeyAction.TypeNumber(1)),
            Key("m_d",   "ㄷ", "2", KeyAction.TypeHangul('ㄷ'), KeyAction.TypeNumber(2)),
            Key("m_g",   "ㄱ", "3", KeyAction.TypeHangul('ㄱ'), KeyAction.TypeNumber(3)),
            Key("m_s",   "ㅅ", "4", KeyAction.TypeHangul('ㅅ'), KeyAction.TypeNumber(4)),
            Key("m_ee",  "ㅔ", "5", KeyAction.TypeHangul('ㅔ'), KeyAction.TypeNumber(5)),
            Key("m_ae",  "ㅐ", "6", KeyAction.TypeHangul('ㅐ'), KeyAction.TypeNumber(6)),
            Key("m_gap", "",   null, KeyAction.TypeText(""), visible = false)
        ),
        // 행 2: ㅁ(7) ㄴ(8) ㅇ(9) ㄹ(0) ㅓ ㅏ ㅣ
        listOf(
            Key("m_m",  "ㅁ", "7", KeyAction.TypeHangul('ㅁ'), KeyAction.TypeNumber(7)),
            Key("m_n",  "ㄴ", "8", KeyAction.TypeHangul('ㄴ'), KeyAction.TypeNumber(8)),
            Key("m_ng", "ㅇ", "9", KeyAction.TypeHangul('ㅇ'), KeyAction.TypeNumber(9)),
            Key("m_r",  "ㄹ", "0", KeyAction.TypeHangul('ㄹ'), KeyAction.TypeNumber(0)),
            Key("m_eo", "ㅓ", null, KeyAction.TypeHangul('ㅓ')),
            Key("m_a",  "ㅏ", null, KeyAction.TypeHangul('ㅏ')),
            Key("m_i",  "ㅣ", null, KeyAction.TypeHangul('ㅣ'))
        ),
        // 행 3: ㅋ ㅌ ㅂ ㅎ ㅜ ㅗ ㅡ ⌫(1.5)
        listOf(
            Key("m_k",   "ㅋ", null, KeyAction.TypeHangul('ㅋ')),
            Key("m_t",   "ㅌ", null, KeyAction.TypeHangul('ㅌ')),
            Key("m_b",   "ㅂ", null, KeyAction.TypeHangul('ㅂ')),
            Key("m_h",   "ㅎ", null, KeyAction.TypeHangul('ㅎ')),
            Key("m_u",   "ㅜ", null, KeyAction.TypeHangul('ㅜ')),
            Key("m_o",   "ㅗ", null, KeyAction.TypeHangul('ㅗ')),
            Key("m_eu",  "ㅡ", null, KeyAction.TypeHangul('ㅡ')),
            Key("del",   "",   null, KeyAction.Backspace, style = KeyStyle.BACKSPACE, widthWeight = 1.5f)
        ),
        // 행 4: EN(3) , Space(3) . 😊 ↵(1.5)
        listOf(
            Key("lang_en", "⌨",  null, KeyAction.SwitchToNextIme, style = KeyStyle.FUNCTION, widthWeight = 3f),
            Key("comma",   ",",  null, KeyAction.TypeText(",")),
            Key("space",   " ",  null, KeyAction.Space, style = KeyStyle.SPACE, widthWeight = 3f),
            Key("period",  ".",  null, KeyAction.TypeText(".")),
            Key("emoji",   "😊", null, KeyAction.ShowEmoji, style = KeyStyle.FUNCTION),
            Key("enter",   "",   null, KeyAction.Enter, style = KeyStyle.ENTER, widthWeight = 1.5f)
        )
    )

    override val rowStartXRatios: List<Float> = listOf(0f, 0f, 0f, 0f)
}
