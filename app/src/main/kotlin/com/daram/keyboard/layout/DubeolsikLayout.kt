package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

/**
 * 두벌식 자판 레이아웃 (QWERTY 배열 기반).
 * DubeolsikAutomata에 TypeRawKey로 소문자 알파벳을 전달한다.
 * Shift 활성 시 엔진이 대문자로 변환하여 쌍자음(ㅃ/ㅉ/ㄸ/ㄲ/ㅆ)을 처리한다.
 *
 * 표준 두벌식 배열:
 *   q=ㅂ  w=ㅈ  e=ㄷ  r=ㄱ  t=ㅅ  y=ㅛ  u=ㅕ  i=ㅑ  o=ㅐ  p=ㅔ
 *   a=ㅁ  s=ㄴ  d=ㅇ  f=ㄹ  g=ㅎ  h=ㅗ  j=ㅓ  k=ㅏ  l=ㅣ
 *   z=ㅋ  x=ㅌ  c=ㅊ  v=ㅍ        b=ㅠ  n=ㅜ  m=ㅡ
 */
object DubeolsikLayout : KeyboardLayout {

    override val rows: List<List<Key>> = listOf(
        // 행 1: q(ㅂ) w(ㅈ) e(ㄷ) r(ㄱ) t(ㅅ) y(ㅛ) u(ㅕ) i(ㅑ) o(ㅐ) p(ㅔ)
        listOf(
            Key("q", "ㅂ", "1", KeyAction.TypeRawKey('q'), KeyAction.TypeNumber(1)),
            Key("w", "ㅈ", "2", KeyAction.TypeRawKey('w'), KeyAction.TypeNumber(2)),
            Key("e", "ㄷ", "3", KeyAction.TypeRawKey('e'), KeyAction.TypeNumber(3)),
            Key("r", "ㄱ", "4", KeyAction.TypeRawKey('r'), KeyAction.TypeNumber(4)),
            Key("t", "ㅅ", "5", KeyAction.TypeRawKey('t'), KeyAction.TypeNumber(5)),
            Key("y", "ㅛ", "6", KeyAction.TypeRawKey('y'), KeyAction.TypeNumber(6)),
            Key("u", "ㅕ", "7", KeyAction.TypeRawKey('u'), KeyAction.TypeNumber(7)),
            Key("i", "ㅑ", "8", KeyAction.TypeRawKey('i'), KeyAction.TypeNumber(8)),
            Key("o", "ㅐ", "9", KeyAction.TypeRawKey('o'), KeyAction.TypeNumber(9)),
            Key("p", "ㅔ", "0", KeyAction.TypeRawKey('p'), KeyAction.TypeNumber(0))
        ),
        // 행 2: a(ㅁ) s(ㄴ) d(ㅇ) f(ㄹ) g(ㅎ) h(ㅗ) j(ㅓ) k(ㅏ) l(ㅣ) — 양쪽 0.5f 패딩으로 가운데 정렬
        listOf(
            Key("dub_pad_l", "", null, KeyAction.TypeText(""), widthWeight = 0.5f, visible = false),
            Key("a", "ㅁ", null, KeyAction.TypeRawKey('a')),
            Key("s", "ㄴ", null, KeyAction.TypeRawKey('s')),
            Key("d", "ㅇ", null, KeyAction.TypeRawKey('d')),
            Key("f", "ㄹ", null, KeyAction.TypeRawKey('f')),
            Key("g", "ㅎ", null, KeyAction.TypeRawKey('g')),
            Key("h", "ㅗ", null, KeyAction.TypeRawKey('h')),
            Key("j", "ㅓ", null, KeyAction.TypeRawKey('j')),
            Key("k", "ㅏ", null, KeyAction.TypeRawKey('k')),
            Key("l", "ㅣ", null, KeyAction.TypeRawKey('l')),
            Key("dub_pad_r", "", null, KeyAction.TypeText(""), widthWeight = 0.5f, visible = false),
        ),
        // 행 3: Shift(1.5) z(ㅋ) x(ㅌ) c(ㅊ) v(ㅍ) b(ㅠ) n(ㅜ) m(ㅡ) ⌫(1.5)
        listOf(
            Key("shift", "", null, KeyAction.Shift, style = KeyStyle.SHIFT, widthWeight = 1.5f),
            Key("z", "ㅋ", null, KeyAction.TypeRawKey('z')),
            Key("x", "ㅌ", null, KeyAction.TypeRawKey('x')),
            Key("c", "ㅊ", null, KeyAction.TypeRawKey('c')),
            Key("v", "ㅍ", null, KeyAction.TypeRawKey('v')),
            Key("b", "ㅠ", null, KeyAction.TypeRawKey('b')),
            Key("n", "ㅜ", null, KeyAction.TypeRawKey('n')),
            Key("m", "ㅡ", null, KeyAction.TypeRawKey('m')),
            Key("del", "", null, KeyAction.Backspace, style = KeyStyle.BACKSPACE, widthWeight = 1.5f)
        ),
        // 행 4: EN(3) , Space(3) . 😊 ↵(1.5)
        listOf(
            Key("lang_en",  "⌨",    null, KeyAction.SwitchToNextIme,   style = KeyStyle.FUNCTION, widthWeight = 3f),
            Key("comma",    ",",    null, KeyAction.TypeText(",")),
            Key("space",    " ",    null, KeyAction.Space, style = KeyStyle.SPACE, widthWeight = 3f),
            Key("period",   ".",    null, KeyAction.TypeText(".")),
            Key("emoji",    "😊",   null, KeyAction.ShowEmoji, style = KeyStyle.FUNCTION),
            Key("enter",    "",     null, KeyAction.Enter, style = KeyStyle.ENTER, widthWeight = 1.5f)
        )
    )

    // 행 2: 양쪽 패딩(각 0.5f)으로 가운데 정렬 — 행 1과 동일한 키 너비(W/10) 확보
    override val rowStartXRatios: List<Float> = listOf(0f, 0f, 0f, 0f)
}
