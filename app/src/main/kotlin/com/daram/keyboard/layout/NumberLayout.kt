package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

object NumberLayout : KeyboardLayout {
    override val rows: List<List<Key>> = listOf(
        // 행 1: - | 1 | 2 | 3 | ⌫
        listOf(
            Key("minus", "-", null, KeyAction.TypeText("-")),
            Key("n1", "1", null, KeyAction.TypeText("1")),
            Key("n2", "2", null, KeyAction.TypeText("2")),
            Key("n3", "3", null, KeyAction.TypeText("3")),
            Key("del", "", null, KeyAction.Backspace, style = KeyStyle.BACKSPACE)
        ),
        // 행 2: 기호 | 4 | 5 | 6 | Space (행2~3 걸침)
        listOf(
            Key("sym", "기호", null, KeyAction.SwitchToSymbol, style = KeyStyle.FUNCTION),
            Key("n4", "4", null, KeyAction.TypeText("4")),
            Key("n5", "5", null, KeyAction.TypeText("5")),
            Key("n6", "6", null, KeyAction.TypeText("6")),
            Key("space", " ", null, KeyAction.Space, style = KeyStyle.SPACE, rowSpan = 2)
        ),
        // 행 3: 😊 | 7 | 8 | 9 | [space 패딩]
        listOf(
            Key("emoji", "😊", null, KeyAction.ShowEmoji, style = KeyStyle.FUNCTION),
            Key("n7", "7", null, KeyAction.TypeText("7")),
            Key("n8", "8", null, KeyAction.TypeText("8")),
            Key("n9", "9", null, KeyAction.TypeText("9")),
            Key("space_pad", "", null, KeyAction.Space, visible = false)
        ),
        // 행 4: 돌아가기 | * | 0 | # | ↵
        listOf(
            Key("ret2", "", null, KeyAction.ReturnToPrev, style = KeyStyle.RETURN),
            Key("star", "*", null, KeyAction.TypeText("*")),
            Key("n0", "0", null, KeyAction.TypeText("0")),
            Key("hash", "#", null, KeyAction.TypeText("#")),
            Key("enter", "", null, KeyAction.Enter, style = KeyStyle.ENTER)
        )
    )
}
