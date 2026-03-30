package com.daram.keyboard.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daram.keyboard.theme.KeyboardTheme

/**
 * 슬랙/트위터 스타일 이모지 입력 패널.
 * - 상단: [한/EN] + 카테고리 탭 스크롤 + [⌫]
 * - 중앙: 선택된 카테고리 이모지 그리드 (수직 스크롤)
 */
class EmojiKeyboardView(
    context: Context,
    private var theme: KeyboardTheme,
    private val onEmojiSelected: (String) -> Unit,
    private val onBackspace: () -> Unit,
    private val onBack: () -> Unit
) : LinearLayout(context) {

    companion object {
        /** 피부색 수정자 코드포인트 범위 (skin tone modifier 지원 여부 판단) */
        private val SKIN_TONE_MODIFIERS = mapOf(
            "light"        to "\uD83C\uDFFB",
            "medium_light" to "\uD83C\uDFFC",
            "medium"       to "\uD83C\uDFFD",
            "medium_dark"  to "\uD83C\uDFFE",
            "dark"         to "\uD83C\uDFFF"
        )

        private fun supportsSkinTone(codePoint: Int): Boolean = codePoint in setOf(
            0x261D, 0x26F9, 0x270A, 0x270B, 0x270C, 0x270D,
            0x1F385, 0x1F3C2, 0x1F3C3, 0x1F3C4, 0x1F3C7,
            0x1F3CA, 0x1F3CB, 0x1F3CC,
            0x1F442, 0x1F443, 0x1F446, 0x1F447, 0x1F448, 0x1F449,
            0x1F44A, 0x1F44B, 0x1F44C, 0x1F44D, 0x1F44E, 0x1F44F,
            0x1F466, 0x1F467, 0x1F468, 0x1F469, 0x1F46A, 0x1F46B,
            0x1F46C, 0x1F46D, 0x1F46E, 0x1F46F, 0x1F470, 0x1F471,
            0x1F472, 0x1F473, 0x1F474, 0x1F475, 0x1F476, 0x1F477,
            0x1F478, 0x1F47C, 0x1F481, 0x1F482, 0x1F483,
            0x1F485, 0x1F486, 0x1F487, 0x1F4AA,
            0x1F574, 0x1F575, 0x1F57A, 0x1F590,
            0x1F595, 0x1F596, 0x1F645, 0x1F646, 0x1F647,
            0x1F64B, 0x1F64C, 0x1F64D, 0x1F64E, 0x1F64F,
            0x1F6A3, 0x1F6B4, 0x1F6B5, 0x1F6B6,
            0x1F6C0, 0x1F6CC, 0x1F90F,
            0x1F918, 0x1F919, 0x1F91A, 0x1F91B, 0x1F91C, 0x1F91D,
            0x1F91E, 0x1F91F, 0x1F926, 0x1F930, 0x1F931, 0x1F932,
            0x1F933, 0x1F934, 0x1F935, 0x1F936, 0x1F937, 0x1F938,
            0x1F939, 0x1F93C, 0x1F93D, 0x1F93E,
            0x1F9B5, 0x1F9B6, 0x1F9B8, 0x1F9B9, 0x1F9BB,
            0x1F9CD, 0x1F9CE, 0x1F9CF,
            0x1F9D1, 0x1F9D2, 0x1F9D3, 0x1F9D4, 0x1F9D5,
            0x1F9D6, 0x1F9D7, 0x1F9D8, 0x1F9D9, 0x1F9DA,
            0x1F9DB, 0x1F9DC, 0x1F9DD,
            0x1FAF0, 0x1FAF1, 0x1FAF2, 0x1FAF3, 0x1FAF4,
            0x1FAF5, 0x1FAF6, 0x1FAF7, 0x1FAF8
        )

        fun applyTone(emoji: String, skinTone: String): String {
            if (skinTone == "none") return emoji
            val modifier = SKIN_TONE_MODIFIERS[skinTone] ?: return emoji
            val cp = emoji.codePointAt(0)
            return if (supportsSkinTone(cp)) emoji + modifier else emoji
        }

        val CATEGORIES = listOf(
            "😀" to listOf(
                "😀","😁","😂","🤣","😃","😄","😅","😆","😇","🥰","😍","🤩","😘","😗","😚","😙",
                "😋","😛","😜","🤪","😝","🤑","🤗","😎","🤔","🤭","🤫","🤥","😶","😐","😑","😬",
                "🙄","😯","😦","😧","😮","😲","😳","🥺","😢","😭","😤","😠","😡","🤬","😈","👿",
                "💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖","😺","😸","😹","😻","😼","😽"
            ),
            "🐶" to listOf(
                "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔",
                "🐧","🐦","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐜",
                "🦟","🦗","🕷️","🦂","🐢","🦎","🐍","🦕","🦖","🦎","🐊","🐅","🐆","🦓","🦍","🦧",
                "🦣","🐘","🦛","🦏","🐪","🐫","🦒","🦘","🦬","🐃","🐂","🐄","🐎","🐖","🐏","🐑"
            ),
            "🍎" to listOf(
                "🍎","🍊","🍋","🍇","🍓","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬",
                "🥒","🌶️","🫑","🧄","🧅","🥔","🍠","🌽","🥕","🫛","🧆","🥜","🌰","🍞","🥐","🥖",
                "🫓","🥨","🧀","🥚","🍳","🥞","🧇","🥓","🥩","🍗","🍖","🌭","🍔","🍟","🍕","🫔",
                "🌮","🌯","🥙","🧆","🥚","🍜","🍝","🍛","🍲","🍣","🍱","🥟","🦪","🍤","🍙","🍚"
            ),
            "⚽" to listOf(
                "⚽","🏀","🏈","⚾","🥎","🏐","🏉","🎾","🏸","🏒","🥅","⛳","🎣","🤿","🎽","🎿",
                "🛷","🥌","🎯","🎱","🎳","🏋️","🤸","🤼","🤺","🏇","⛷️","🏂","🪂","🏄","🚣","🧗",
                "🚴","🏊","🤽","🧘","🛹","🛼","🪃","🏹","🎭","🎨","🎬","🎤","🎧","🎼","🎹","🪘",
                "🎷","🎺","🎸","🪕","🎻","🥁","🪗","🎲","♟️","🎯","🎳","🎰","🎮","🕹️","🃏","🀄"
            ),
            "🚗" to listOf(
                "🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🚚","🚛","🚜","🛵","🚲","🛴",
                "✈️","🚀","🛸","🚁","🚂","🚢","⛵","🛥️","🚤","🛶","🛺","🚡","🚠","🚟","🚃","🚋",
                "🚝","🚄","🚅","🚈","🚞","🚇","🚊","🚉","🛫","🛬","🛩️","💺","🚁","🚟","🚠","🚡",
                "🛰️","🚀","🛸","⛽","🚧","⚓","🪝","⛽","🚦","🚥","🛑","🚨","🛤️","🛣️","🗺️","🧭"
            ),
            "🏠" to listOf(
                "🏠","🏡","🏢","🏣","🏤","🏥","🏦","🏨","🏩","🏪","🏫","🏬","🏭","🏯","🏰","💒",
                "🗼","🗽","⛪","🕌","🕍","🛕","🕋","⛩️","🗾","🎌","🏔️","⛰️","🌋","🗻","🏕️","🏖️",
                "🏗️","🏘️","🏚️","🌁","🌃","🏙️","🌄","🌅","🌆","🌇","🌉","🏞️","🎠","🎡","🎢","💈",
                "🎪","🤹","🎭","🖼️","🎨","🎰","🚂","🚃","🚄","🚅","🚆","🚇","🚈","🚉","🚊","🚝"
            ),
            "💡" to listOf(
                "💡","🔦","🕯️","📱","💻","⌨️","🖥️","🖨️","🖱️","💾","💿","📀","📷","📸","📹","📽️",
                "📞","☎️","📟","📠","📺","📻","🧭","⏰","⌚","⏱️","⏲️","🕰️","📡","🔋","🔌","💿",
                "🔧","🔨","⚒️","🛠️","⛏️","🔩","🪛","🔑","🗝️","🔐","🔒","🔓","🚪","🪞","🪟","🛋️",
                "🪑","🚽","🚿","🛁","🪠","🧴","🧷","🧹","🧺","🧻","🪣","🧼","🫧","🧽","🧯","🛒"
            ),
            "❤️" to listOf(
                "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖",
                "💘","💝","💟","☮️","✝️","☪️","🕉️","☯️","✡️","🔯","🛐","⚛️","🆔","♾️","🔰","♻️",
                "✅","❎","🆕","🆓","🆒","🆗","🆙","🆘","❌","⭕","🛑","⛔","📛","🚫","💯","💢",
                "♨️","🌀","🔔","🔕","🎵","🎶","✨","🎉","🎊","🎈","🎁","🎗️","🎀","🎟️","🎫","🏷️"
            )
        )
    }

    private var selectedCategory = 0
    private var skinTone = "none"
    private val emojiAdapter = EmojiAdapter()
    private val categoryTabViews = mutableListOf<TextView>()
    private val recyclerView: RecyclerView
    private lateinit var backBtn: TextView
    private lateinit var tabRow: LinearLayout

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.backgroundColor)

        // 상단 탭 바: [한/EN] + [탭 스크롤] + [⌫]
        val tabBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        val tabBarHeight = (48 * resources.displayMetrics.density).toInt()
        val btnWidth = (52 * resources.displayMetrics.density).toInt()

        backBtn = TextView(context).apply {
            text = "한"
            textSize = 15f
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            setOnClickListener { onBack() }
        }

        val tabScrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        tabRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        CATEGORIES.forEachIndexed { idx, (icon, _) ->
            val tab = TextView(context).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                val dp8 = (8 * resources.displayMetrics.density).toInt()
                val dp10 = (10 * resources.displayMetrics.density).toInt()
                setPadding(dp10, dp8, dp10, dp8)
                setOnClickListener { selectCategory(idx) }
            }
            categoryTabViews.add(tab)
            tabRow.addView(tab, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        tabScrollView.addView(tabRow)

        val delBtn = TextView(context).apply {
            text = "\u232B"
            textSize = 20f
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            setOnClickListener { onBackspace() }
        }

        tabBar.addView(backBtn, LinearLayout.LayoutParams(btnWidth, ViewGroup.LayoutParams.MATCH_PARENT))
        tabBar.addView(tabScrollView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        tabBar.addView(delBtn, LinearLayout.LayoutParams(btnWidth, ViewGroup.LayoutParams.MATCH_PARENT))

        addView(tabBar, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tabBarHeight))

        // 이모지 그리드 (수직 스크롤)
        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 8)
            adapter = emojiAdapter
            clipToPadding = false
        }
        addView(recyclerView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        selectCategory(0)
        applyTheme(theme)
    }

    fun applyTheme(newTheme: KeyboardTheme) {
        theme = newTheme
        setBackgroundColor(theme.backgroundColor)
        updateTabBarColors()
        updateTabColors()
    }

    private fun updateTabBarColors() {
        if (!::backBtn.isInitialized) return
        backBtn.setTextColor(theme.keyLabelColor)
        backBtn.setBackgroundColor(theme.keyFunctionBackground)
        // delBtn — tabBar의 마지막 자식
        val tabBar = getChildAt(0) as? LinearLayout ?: return
        val delBtn = tabBar.getChildAt(2) as? TextView ?: return
        delBtn.setTextColor(theme.keyLabelColor)
        delBtn.setBackgroundColor(theme.keyFunctionBackground)
        // tabRow 배경
        (tabBar.getChildAt(1) as? HorizontalScrollView)?.setBackgroundColor(theme.backgroundColor)
    }

    private fun updateTabColors() {
        categoryTabViews.forEachIndexed { idx, tab ->
            tab.setBackgroundColor(
                if (idx == selectedCategory) theme.keyNormalBackground else theme.keyFunctionBackground
            )
            tab.setTextColor(theme.keyLabelColor)
        }
    }

    fun setBackLabel(label: String) {
        if (::backBtn.isInitialized) backBtn.text = label
    }

    fun setSkinTone(tone: String) {
        skinTone = tone
        emojiAdapter.notifyDataSetChanged()
    }

    fun selectCategory(idx: Int) {
        selectedCategory = idx
        emojiAdapter.setEmojis(CATEGORIES[idx].second)
        recyclerView.scrollToPosition(0)
        updateTabColors()
    }

    fun setGestureNavPadding(bottomPx: Int) {
        recyclerView.setPadding(0, 0, 0, bottomPx)
    }

    inner class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {
        private var emojis: List<String> = emptyList()

        fun setEmojis(list: List<String>) {
            emojis = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
            val cellSize = if (parent.width > 0) parent.width / 8
                           else (40 * resources.displayMetrics.density).toInt()
            val tv = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellSize.coerceAtLeast(1))
                gravity = Gravity.CENTER
                textSize = 22f
                isFocusable = true
                isClickable = true
            }
            return EmojiViewHolder(tv)
        }

        override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
            val emoji = emojis[position]
            val displayEmoji = applyTone(emoji, skinTone)
            (holder.itemView as TextView).text = displayEmoji
            holder.itemView.setOnClickListener { onEmojiSelected(displayEmoji) }
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        override fun getItemCount() = emojis.size

        inner class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
