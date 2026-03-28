package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

/**
 * 이모지 레이아웃. 8개 카테고리를 페이지로 분리.
 * 하단 행에 카테고리 탭 버튼을 배치한다.
 * 각 카테고리 5행 × 8열 = 40개 이모지 표시.
 */
class EmojiLayout private constructor(val categoryIndex: Int) : KeyboardLayout {

    companion object {
        val categories = listOf("😀", "🐶", "🍎", "⚽", "🚗", "🏠", "💡", "❤️")
        val CATEGORY_COUNT = categories.size
        val all = categories.indices.map { EmojiLayout(it) }

        fun of(index: Int) = all[index.coerceIn(0, all.size - 1)]
    }

    // 카테고리별 이모지 목록 (5행 × 8열 = 40개)
    private val emojiData: List<List<String>> = when (categoryIndex) {
        0 -> listOf( // 얼굴/감정
            listOf("😀","😁","😂","🤣","😃","😄","😅","😆"),
            listOf("😇","🥰","😍","🤩","😘","😗","😚","😙"),
            listOf("😋","😛","😜","🤪","😝","🤑","🤗","😎"),
            listOf("🤔","🤭","🤫","🤥","😶","😐","😑","😬"),
            listOf("🙄","😯","😦","😧","😮","😲","😳","🥺")
        )
        1 -> listOf( // 동물
            listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼"),
            listOf("🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔"),
            listOf("🐧","🐦","🦆","🦅","🦉","🦇","🐺","🐗"),
            listOf("🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐜"),
            listOf("🦟","🦗","🕷️","🦂","🐢","🦎","🐍","🦕")
        )
        2 -> listOf( // 음식
            listOf("🍎","🍊","🍋","🍇","🍓","🍒","🍑","🥭"),
            listOf("🍕","🍔","🌮","🌯","🥪","🍜","🍣","🍱"),
            listOf("🍦","🍧","🍨","🎂","🍰","🧁","🍩","🍪"),
            listOf("🥐","🍞","🥖","🥨","🧀","🥚","🍳","🥞"),
            listOf("🍗","🍖","🌭","🍟","🥓","🥩","🍛","🍲")
        )
        3 -> listOf( // 스포츠/활동
            listOf("⚽","🏀","🏈","⚾","🥎","🏐","🏉","🎾"),
            listOf("🏸","🏒","🥅","⛳","🎣","🤿","🎽","🎿"),
            listOf("🛷","🥌","🎯","🎱","🎳","🏋️","🤸","🤼"),
            listOf("🤺","🏇","⛷️","🏂","🪂","🏄","🚣","🧗"),
            listOf("🚴","🏊","🤽","🧘","🛹","🛼","🪃","🏹")
        )
        4 -> listOf( // 교통/탈것
            listOf("🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑"),
            listOf("🚒","🚐","🚚","🚛","🚜","🛵","🚲","🛴"),
            listOf("✈️","🚀","🛸","🚁","🚂","🚢","⛵","🛥️"),
            listOf("🚤","🛶","🛺","🚡","🚠","🚟","🚃","🚋"),
            listOf("🚝","🚄","🚅","🚈","🚞","🚇","🚊","🚉")
        )
        5 -> listOf( // 장소/건물
            listOf("🏠","🏡","🏢","🏣","🏤","🏥","🏦","🏨"),
            listOf("🏩","🏪","🏫","🏬","🏭","🏯","🏰","💒"),
            listOf("🗼","🗽","⛪","🕌","🕍","🛕","🕋","⛩️"),
            listOf("🗾","🎌","🏔️","⛰️","🌋","🗻","🏕️","🏖️"),
            listOf("🏗️","🏘️","🏚️","🌁","🌃","🏙️","🌄","🌅")
        )
        6 -> listOf( // 사물/도구
            listOf("💡","🔦","🕯️","📱","💻","⌨️","🖥️","🖨️"),
            listOf("🖱️","💾","💿","📀","📷","📸","📹","📽️"),
            listOf("📞","☎️","📟","📠","📺","📻","🧭","⏰"),
            listOf("⌚","⏱️","⏲️","🕰️","📡","🔋","🔌","💿"),
            listOf("🔧","🔨","⚒️","🛠️","⛏️","🔩","🪛","🔑")
        )
        else -> listOf( // 기호/하트
            listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍"),
            listOf("💕","💞","💓","💗","💖","💘","💝","💟"),
            listOf("☮️","✝️","☪️","🕉️","☯️","✡️","🔯","🛐"),
            listOf("♈","♉","♊","♋","♌","♍","♎","♏"),
            listOf("♐","♑","♒","♓","⛎","🔀","🔁","🔂")
        )
    }

    override val isAuxiliary = true

    override val rows: List<List<Key>> get() {
        val emojiRows = emojiData.mapIndexed { rowIdx, emojis ->
            emojis.mapIndexed { colIdx, emoji ->
                Key("em_${categoryIndex}_${rowIdx}_$colIdx", emoji, null, KeyAction.TypeText(emoji))
            }
        }

        // 하단: 뒤로가기, 카테고리 탭 8개, ⌫
        val categoryTabs = categories.mapIndexed { idx, icon ->
            Key(
                "cat_$idx", icon, null,
                KeyAction.SwitchEmojiCategory(idx),
                style = if (idx == categoryIndex) KeyStyle.NORMAL else KeyStyle.FUNCTION,
                widthWeight = 0.8f
            )
        }
        val bottomRow = listOf(
            Key("em_back", "←", null, KeyAction.ReturnToPrev, style = KeyStyle.RETURN, widthWeight = 1.2f)
        ) + categoryTabs + listOf(
            Key("del", "", null, KeyAction.Backspace, style = KeyStyle.BACKSPACE, widthWeight = 1.2f)
        )

        return emojiRows + listOf(bottomRow)
    }
}
