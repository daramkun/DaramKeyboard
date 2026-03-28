package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

object NaratgulLayout : KeyboardLayout {
    override val secondaryLabelAlignment = SecondaryLabelAlignment.BOTTOM_RIGHT

    override val rows: List<List<Key>> = listOf(
        // 행 1: 123 | ㄱ(1) | ㄴ(2) | ㅏㅓ(3) | ⌫
        listOf(
            Key("num", "123", null, KeyAction.SwitchToNumber, style = KeyStyle.FUNCTION),
            Key("ga", "ㄱ", "1", KeyAction.TypeHangul('ㄱ'), KeyAction.TypeNumber(1)),
            Key("na", "ㄴ", "2", KeyAction.TypeHangul('ㄴ'), KeyAction.TypeNumber(2)),
            Key("ao", "ㅏㅓ", "3", KeyAction.TypeHangul('ㅏ'), KeyAction.TypeNumber(3)),
            Key("del", "", null, KeyAction.Backspace, style = KeyStyle.BACKSPACE)
        ),
        // 행 2: 기호 | ㄹ(4) | ㅁ(5) | ㅗㅜ(6) | Space (행2~3에 걸침)
        listOf(
            Key("sym", "기호", null, KeyAction.SwitchToSymbol, style = KeyStyle.FUNCTION),
            Key("ra", "ㄹ", "4", KeyAction.TypeHangul('ㄹ'), KeyAction.TypeNumber(4)),
            Key("ma", "ㅁ", "5", KeyAction.TypeHangul('ㅁ'), KeyAction.TypeNumber(5)),
            Key("ou", "ㅗㅜ", "6", KeyAction.TypeHangul('ㅗ'), KeyAction.TypeNumber(6)),
            Key("space", " ", null, KeyAction.Space, style = KeyStyle.SPACE, rowSpan = 2)
        ),
        // 행 3: 😊 | ㅅ(7) | ㅇ(8) | ㅣ(9) | [space rowSpan 자리 확보용 패딩]
        listOf(
            Key("emoji", "😊", null, KeyAction.ShowEmoji, style = KeyStyle.FUNCTION),
            Key("sa", "ㅅ", "7", KeyAction.TypeHangul('ㅅ'), KeyAction.TypeNumber(7)),
            Key("eo", "ㅇ", "8", KeyAction.TypeHangul('ㅇ'), KeyAction.TypeNumber(8)),
            Key("i", "ㅣ", "9", KeyAction.TypeHangul('ㅣ'), KeyAction.TypeNumber(9)),
            Key("space_pad", "", null, KeyAction.Space, visible = false)
        ),
        // 행 4: 한↔EN(꾹=자판변경) | 획추가 | —(0) | 쌍자음 | ↵
        listOf(
            Key("lang", "⌨", null, KeyAction.SwitchToNextIme, KeyAction.SwitchKoreanType, style = KeyStyle.FUNCTION),
            Key("stroke", "획추가", null, KeyAction.AddStroke, style = KeyStyle.FUNCTION),
            Key("eu", "ㅡ", "0", KeyAction.TypeHangul('ㅡ'), KeyAction.TypeNumber(0)),
            Key("double", "쌍자음", null, KeyAction.ToggleDouble, style = KeyStyle.FUNCTION),
            Key("enter", "", null, KeyAction.Enter, style = KeyStyle.ENTER)
        )
    )
}
