package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

object QwertyLayout : KeyboardLayout {
    override val returnButtonLabel = "EN"

    override val rows: List<List<Key>> = listOf(
        // 행 1: q(1) w(2) e(3) r(4) t(5) y(6) u(7) i(8) o(9) p(0)
        listOf(
            Key("q", "q", "1", KeyAction.TypeText("q"), KeyAction.TypeNumber(1)),
            Key("w", "w", "2", KeyAction.TypeText("w"), KeyAction.TypeNumber(2)),
            Key("e", "e", "3", KeyAction.TypeText("e"), KeyAction.TypeNumber(3)),
            Key("r", "r", "4", KeyAction.TypeText("r"), KeyAction.TypeNumber(4)),
            Key("t", "t", "5", KeyAction.TypeText("t"), KeyAction.TypeNumber(5)),
            Key("y", "y", "6", KeyAction.TypeText("y"), KeyAction.TypeNumber(6)),
            Key("u", "u", "7", KeyAction.TypeText("u"), KeyAction.TypeNumber(7)),
            Key("i", "i", "8", KeyAction.TypeText("i"), KeyAction.TypeNumber(8)),
            Key("o", "o", "9", KeyAction.TypeText("o"), KeyAction.TypeNumber(9)),
            Key("p", "p", "0", KeyAction.TypeText("p"), KeyAction.TypeNumber(0))
        ),
        // 행 2: a s d f g h j k l  (행1 키 너비의 절반만큼 들여쓰기)
        listOf(
            Key("a", "a", null, KeyAction.TypeText("a")),
            Key("s", "s", null, KeyAction.TypeText("s")),
            Key("d", "d", null, KeyAction.TypeText("d")),
            Key("f", "f", null, KeyAction.TypeText("f")),
            Key("g", "g", null, KeyAction.TypeText("g")),
            Key("h", "h", null, KeyAction.TypeText("h")),
            Key("j", "j", null, KeyAction.TypeText("j")),
            Key("k", "k", null, KeyAction.TypeText("k")),
            Key("l", "l", null, KeyAction.TypeText("l"))
        ),
        // 행 3: Shift(1.5f) z x c v b n m ⌫(1.5f)
        listOf(
            Key("shift", "", null, KeyAction.Shift, style = KeyStyle.SHIFT, widthWeight = 1.5f),
            Key("z", "z", null, KeyAction.TypeText("z")),
            Key("x", "x", null, KeyAction.TypeText("x")),
            Key("c", "c", null, KeyAction.TypeText("c")),
            Key("v", "v", null, KeyAction.TypeText("v")),
            Key("b", "b", null, KeyAction.TypeText("b")),
            Key("n", "n", null, KeyAction.TypeText("n")),
            Key("m", "m", null, KeyAction.TypeText("m")),
            Key("del", "", null, KeyAction.Backspace, style = KeyStyle.BACKSPACE, widthWeight = 1.5f)
        ),
        // 행 4: 한(1.5f) !@#(1.5f) ,(1f) Space(3f) .(1f) 😊(1f) ↵(1.5f)
        listOf(
            Key("lang_ko", "⌨",  null, KeyAction.SwitchToNextIme,    style = KeyStyle.FUNCTION, widthWeight = 1.5f),
            Key("qsym",    "!@#", null, KeyAction.SwitchToQwertySymbol,style = KeyStyle.FUNCTION, widthWeight = 1.5f),
            Key("comma",   ",",   null, KeyAction.TypeText(",")),
            Key("space",   " ",   null, KeyAction.Space, style = KeyStyle.SPACE, widthWeight = 3f),
            Key("period",  ".",   null, KeyAction.TypeText(".")),
            Key("emoji",   "😊",  null, KeyAction.ShowEmoji, style = KeyStyle.FUNCTION),
            Key("enter",   "",    null, KeyAction.Enter, style = KeyStyle.ENTER, widthWeight = 1.5f)
        )
    )

    // 행 2: 행1 키 너비(1/10)의 절반 = 전체 너비의 0.05 만큼 들여쓰기
    override val rowStartXRatios: List<Float> = listOf(0f, 0.05f, 0f, 0f)
}
