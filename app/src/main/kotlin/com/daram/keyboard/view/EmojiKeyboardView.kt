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
            (holder.itemView as TextView).text = emoji
            holder.itemView.setOnClickListener { onEmojiSelected(emoji) }
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        override fun getItemCount() = emojis.size

        inner class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
