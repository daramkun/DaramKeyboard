package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

/**
 * 단모음 자판 레이아웃 (QWERTY 배열 기반).
 * DanmoemAutomata에 TypeRawKey로 소문자 알파벳을 전달한다.
 * 동일 키 두 번 입력으로 쌍자음·이중모음을 생성한다 (Shift 불필요).
 *
 * 단모음 배열 (DubeolsikLayout과 모음 위치가 다름):
 *   q=ㅂ  w=ㅈ  e=ㄷ  r=ㄱ  t=ㅅ  y=ㅗ(ㅛ)  u=ㅐ(ㅒ)  i=ㅔ(ㅖ)  o=o  p=p
 *   a=ㅁ  s=ㄴ  d=ㅇ  f=ㄹ  g=ㅎ  h=ㅓ(ㅕ)  j=ㅏ(ㅑ)  k=ㅣ  l=l
 *   z=ㅋ  x=ㅌ  c=ㅊ  v=ㅍ  b=ㅜ(ㅠ)  n=ㅡ  m=m
 */
object DanmoemLayout : KeyboardLayout {

    override val rows: List<List<Key>> = listOf(
        // 행 1: q(ㅂ) w(ㅈ) e(ㄷ) r(ㄱ) t(ㅅ) y(ㅗ) u(ㅐ) i(ㅔ) o p
        listOf(
            Key("q", "ㅂ", "1", KeyAction.TypeRawKey('q'), KeyAction.TypeNumber(1)),
            Key("w", "ㅈ", "2", KeyAction.TypeRawKey('w'), KeyAction.TypeNumber(2)),
            Key("e", "ㄷ", "3", KeyAction.TypeRawKey('e'), KeyAction.TypeNumber(3)),
            Key("r", "ㄱ", "4", KeyAction.TypeRawKey('r'), KeyAction.TypeNumber(4)),
            Key("t", "ㅅ", "5", KeyAction.TypeRawKey('t'), KeyAction.TypeNumber(5)),
            Key("y", "ㅗ", "6", KeyAction.TypeRawKey('y'), KeyAction.TypeNumber(6)),
            Key("u", "ㅐ", "7", KeyAction.TypeRawKey('u'), KeyAction.TypeNumber(7)),
            Key("i", "ㅔ", "8", KeyAction.TypeRawKey('i'), KeyAction.TypeNumber(8)),
            Key("o", "o",  "9", KeyAction.TypeRawKey('o'), KeyAction.TypeNumber(9)),
            Key("p", "p",  "0", KeyAction.TypeRawKey('p'), KeyAction.TypeNumber(0))
        ),
        // 행 2: a(ㅁ) s(ㄴ) d(ㅇ) f(ㄹ) g(ㅎ) h(ㅓ) j(ㅏ) k(ㅣ) l
        listOf(
            Key("a", "ㅁ", null, KeyAction.TypeRawKey('a')),
            Key("s", "ㄴ", null, KeyAction.TypeRawKey('s')),
            Key("d", "ㅇ", null, KeyAction.TypeRawKey('d')),
            Key("f", "ㄹ", null, KeyAction.TypeRawKey('f')),
            Key("g", "ㅎ", null, KeyAction.TypeRawKey('g')),
            Key("h", "ㅓ", null, KeyAction.TypeRawKey('h')),
            Key("j", "ㅏ", null, KeyAction.TypeRawKey('j')),
            Key("k", "ㅣ", null, KeyAction.TypeRawKey('k')),
            Key("l", "l",  null, KeyAction.TypeRawKey('l'))
        ),
        // 행 3: z(ㅋ) x(ㅌ) c(ㅊ) v(ㅍ) b(ㅜ) n(ㅡ) m ⌫(1.5)
        listOf(
            Key("z",   "ㅋ", null, KeyAction.TypeRawKey('z')),
            Key("x",   "ㅌ", null, KeyAction.TypeRawKey('x')),
            Key("c",   "ㅊ", null, KeyAction.TypeRawKey('c')),
            Key("v",   "ㅍ", null, KeyAction.TypeRawKey('v')),
            Key("b",   "ㅜ", null, KeyAction.TypeRawKey('b')),
            Key("n",   "ㅡ", null, KeyAction.TypeRawKey('n')),
            Key("m",   "m",  null, KeyAction.TypeRawKey('m')),
            Key("del", "",   null, KeyAction.Backspace, style = KeyStyle.BACKSPACE, widthWeight = 3f)
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

    override val rowStartXRatios: List<Float> = listOf(0f, 0.05f, 0.1f, 0f)
}
