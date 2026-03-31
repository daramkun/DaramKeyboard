package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

/**
 * 무이128 자판 레이아웃 (두벌식 기반 12자음+8모음 멀티탭).
 * Mue128Automata에 TypeHangul로 한글 자모를 전달한다.
 * 동일 키 두 번 입력으로 쌍자음·이중모음을 생성한다.
 *
 *   자음 멀티탭: ㄱ(ㄲ), ㄷ(ㄸ), ㅂ(ㅃ), ㅅ(ㅆ), ㅈ(ㅉ), ㅌ(ㅊ), ㅎ(ㅍ), ㄴ, ㄹ, ㅁ, ㅇ, ㅋ
 *   모음 멀티탭: ㅏ(ㅑ), ㅓ(ㅕ), ㅗ(ㅛ), ㅜ(ㅠ), ㅐ(ㅒ), ㅔ(ㅖ), ㅣ, ㅡ
 */
object Mue128Layout : KeyboardLayout {

    override val secondaryLabelAlignment = SecondaryLabelAlignment.BOTTOM_RIGHT

    override val rows: List<List<Key>> = listOf(
        // 행 1: ㅂ(ㅃ) ㅈ(ㅉ) ㄷ(ㄸ) ㄱ(ㄲ) ㅅ(ㅆ) | ㅔ(ㅖ) ㅐ(ㅒ) ㅓ(ㅕ) ㅏ(ㅑ) ㅣ
        listOf(
            Key("m_b",  "ㅂ", "1", KeyAction.TypeHangul('ㅂ'), KeyAction.TypeNumber(1)),
            Key("m_j",  "ㅈ", "2", KeyAction.TypeHangul('ㅈ'), KeyAction.TypeNumber(2)),
            Key("m_d",  "ㄷ", "3", KeyAction.TypeHangul('ㄷ'), KeyAction.TypeNumber(3)),
            Key("m_g",  "ㄱ", "4", KeyAction.TypeHangul('ㄱ'), KeyAction.TypeNumber(4)),
            Key("m_s",  "ㅅ", "5", KeyAction.TypeHangul('ㅅ'), KeyAction.TypeNumber(5)),
            Key("m_ee", "ㅔ", "6", KeyAction.TypeHangul('ㅔ'), KeyAction.TypeNumber(6)),
            Key("m_ae", "ㅐ", "7", KeyAction.TypeHangul('ㅐ'), KeyAction.TypeNumber(7)),
            Key("m_eo", "ㅓ", "8", KeyAction.TypeHangul('ㅓ'), KeyAction.TypeNumber(8)),
            Key("m_a",  "ㅏ", "9", KeyAction.TypeHangul('ㅏ'), KeyAction.TypeNumber(9)),
            Key("m_i",  "ㅣ", "0", KeyAction.TypeHangul('ㅣ'), KeyAction.TypeNumber(0))
        ),
        // 행 2: ㅁ ㄴ ㅇ ㄹ ㅎ(ㅍ) | ㅗ(ㅛ) ㅜ(ㅠ) ㅡ
        listOf(
            Key("m_m",  "ㅁ", null, KeyAction.TypeHangul('ㅁ')),
            Key("m_n",  "ㄴ", null, KeyAction.TypeHangul('ㄴ')),
            Key("m_ng", "ㅇ", null, KeyAction.TypeHangul('ㅇ')),
            Key("m_r",  "ㄹ", null, KeyAction.TypeHangul('ㄹ')),
            Key("m_h",  "ㅎ", null, KeyAction.TypeHangul('ㅎ')),
            Key("m_o",  "ㅗ", null, KeyAction.TypeHangul('ㅗ')),
            Key("m_u",  "ㅜ", null, KeyAction.TypeHangul('ㅜ')),
            Key("m_eu", "ㅡ", null, KeyAction.TypeHangul('ㅡ'))
        ),
        // 행 3: ㅌ(ㅊ×2)(4) ㅋ(4) ⌫(2)
        listOf(
            Key("m_t",  "ㅌ", null, KeyAction.TypeHangul('ㅌ'), widthWeight = 4f),
            Key("m_k",  "ㅋ", null, KeyAction.TypeHangul('ㅋ'), widthWeight = 4f),
            Key("del",  "",   null, KeyAction.Backspace, style = KeyStyle.BACKSPACE, widthWeight = 2f)
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

    override val rowStartXRatios: List<Float> = listOf(0f, 0.05f, 0f, 0f)
}
