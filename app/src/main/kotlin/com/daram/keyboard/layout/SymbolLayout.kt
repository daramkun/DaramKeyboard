package com.daram.keyboard.layout

import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle

class SymbolLayout private constructor(val page: Int) : KeyboardLayout {

    companion object {
        val Page1 = SymbolLayout(0)
        val Page2 = SymbolLayout(1)
        val Page3 = SymbolLayout(2)

        fun page(index: Int) = when (index) {
            1 -> Page2
            2 -> Page3
            else -> Page1
        }

        fun nextPage(current: SymbolLayout) = page((current.page + 1) % 3)
    }

    override val rows: List<List<Key>> get() = when (page) {
        0 -> page1Rows
        1 -> page2Rows
        else -> page3Rows
    }

    private fun bottomRow(pageLabel: String) = listOf(
        Key("ret",      "",   null, KeyAction.ReturnToPrev, style = KeyStyle.RETURN, widthWeight = 1.5f),
        Key("s_comma",  ",",  null, KeyAction.TypeText(",")),
        Key("space",    " ",  null, KeyAction.Space, style = KeyStyle.SPACE, widthWeight = 4f),
        Key("s_period", ".",  null, KeyAction.TypeText(".")),
        Key("sym_next", pageLabel, null, KeyAction.SymbolNextPage, style = KeyStyle.FUNCTION, widthWeight = 1.5f)
    )

    private val page1Rows = listOf(
        listOf(
            Key("s_at",     "@",  null, KeyAction.TypeText("@")),
            Key("s_hash",   "#",  null, KeyAction.TypeText("#")),
            Key("s_dollar", "$",  null, KeyAction.TypeText("$")),
            Key("s_pct",    "%",  null, KeyAction.TypeText("%")),
            Key("s_amp",    "&",  null, KeyAction.TypeText("&")),
            Key("s_star",   "*",  null, KeyAction.TypeText("*")),
            Key("s_lp",     "(",  null, KeyAction.TypeText("(")),
            Key("s_rp",     ")",  null, KeyAction.TypeText(")")),
            Key("del",      "",   null, KeyAction.Backspace, style = KeyStyle.BACKSPACE)
        ),
        listOf(
            Key("s_plus",   "+",  null, KeyAction.TypeText("+")),
            Key("s_minus",  "-",  null, KeyAction.TypeText("-")),
            Key("s_eq",     "=",  null, KeyAction.TypeText("=")),
            Key("s_slash",  "/",  null, KeyAction.TypeText("/")),
            Key("s_colon",  ":",  null, KeyAction.TypeText(":")),
            Key("s_semi",   ";",  null, KeyAction.TypeText(";")),
            Key("s_excl",   "!",  null, KeyAction.TypeText("!")),
            Key("s_quest",  "?",  null, KeyAction.TypeText("?")),
            Key("s_dquote", "\"", null, KeyAction.TypeText("\""))
        ),
        listOf(
            Key("s_lt",     "<",  null, KeyAction.TypeText("<")),
            Key("s_gt",     ">",  null, KeyAction.TypeText(">")),
            Key("s_lb",     "[",  null, KeyAction.TypeText("[")),
            Key("s_rb",     "]",  null, KeyAction.TypeText("]")),
            Key("s_lc",     "{",  null, KeyAction.TypeText("{")),
            Key("s_rc",     "}",  null, KeyAction.TypeText("}")),
            Key("s_caret",  "^",  null, KeyAction.TypeText("^")),
            Key("s_tilde",  "~",  null, KeyAction.TypeText("~")),
            Key("s_pipe",   "|",  null, KeyAction.TypeText("|"))
        ),
        bottomRow("▶2")
    )

    private val page2Rows = listOf(
        listOf(
            Key("s2_back",  "\\", null, KeyAction.TypeText("\\")),
            Key("s2_under", "_",  null, KeyAction.TypeText("_")),
            Key("s2_grave", "`",  null, KeyAction.TypeText("`")),
            Key("s2_apos",  "'",  null, KeyAction.TypeText("'")),
            Key("s2_lq",    "\u201C", null, KeyAction.TypeText("\u201C")),
            Key("s2_rq",    "\u201D", null, KeyAction.TypeText("\u201D")),
            Key("s2_ls",    "\u2018", null, KeyAction.TypeText("\u2018")),
            Key("s2_rs",    "\u2019", null, KeyAction.TypeText("\u2019")),
            Key("del",      "",   null, KeyAction.Backspace, style = KeyStyle.BACKSPACE)
        ),
        listOf(
            Key("s2_cent",  "¢",  null, KeyAction.TypeText("¢")),
            Key("s2_pound", "£",  null, KeyAction.TypeText("£")),
            Key("s2_euro",  "€",  null, KeyAction.TypeText("€")),
            Key("s2_yen",   "¥",  null, KeyAction.TypeText("¥")),
            Key("s2_won",   "₩",  null, KeyAction.TypeText("₩")),
            Key("s2_deg",   "°",  null, KeyAction.TypeText("°")),
            Key("s2_copy",  "©",  null, KeyAction.TypeText("©")),
            Key("s2_reg",   "®",  null, KeyAction.TypeText("®")),
            Key("s2_tm",    "™",  null, KeyAction.TypeText("™"))
        ),
        listOf(
            Key("s2_bull",  "•",  null, KeyAction.TypeText("•")),
            Key("s2_ellip", "…",  null, KeyAction.TypeText("…")),
            Key("s2_dash",  "—",  null, KeyAction.TypeText("—")),
            Key("s2_ndash", "–",  null, KeyAction.TypeText("–")),
            Key("s2_times", "×",  null, KeyAction.TypeText("×")),
            Key("s2_div",   "÷",  null, KeyAction.TypeText("÷")),
            Key("s2_neq",   "≠",  null, KeyAction.TypeText("≠")),
            Key("s2_lteq",  "≤",  null, KeyAction.TypeText("≤")),
            Key("s2_gteq",  "≥",  null, KeyAction.TypeText("≥"))
        ),
        bottomRow("▶3")
    )

    private val page3Rows = listOf(
        listOf(
            Key("s3_f12",   "½",  null, KeyAction.TypeText("½")),
            Key("s3_f14",   "¼",  null, KeyAction.TypeText("¼")),
            Key("s3_f34",   "¾",  null, KeyAction.TypeText("¾")),
            Key("s3_sup1",  "¹",  null, KeyAction.TypeText("¹")),
            Key("s3_sup2",  "²",  null, KeyAction.TypeText("²")),
            Key("s3_sup3",  "³",  null, KeyAction.TypeText("³")),
            Key("s3_pm",    "±",  null, KeyAction.TypeText("±")),
            Key("s3_inf",   "∞",  null, KeyAction.TypeText("∞")),
            Key("del",      "",   null, KeyAction.Backspace, style = KeyStyle.BACKSPACE)
        ),
        listOf(
            Key("s3_alpha", "α",  null, KeyAction.TypeText("α")),
            Key("s3_beta",  "β",  null, KeyAction.TypeText("β")),
            Key("s3_gamma", "γ",  null, KeyAction.TypeText("γ")),
            Key("s3_delta", "δ",  null, KeyAction.TypeText("δ")),
            Key("s3_pi",    "π",  null, KeyAction.TypeText("π")),
            Key("s3_sigma", "σ",  null, KeyAction.TypeText("σ")),
            Key("s3_omega", "ω",  null, KeyAction.TypeText("ω")),
            Key("s3_mu",    "μ",  null, KeyAction.TypeText("μ")),
            Key("s3_theta", "θ",  null, KeyAction.TypeText("θ"))
        ),
        listOf(
            Key("s3_arl",   "←",  null, KeyAction.TypeText("←")),
            Key("s3_arr",   "→",  null, KeyAction.TypeText("→")),
            Key("s3_aru",   "↑",  null, KeyAction.TypeText("↑")),
            Key("s3_ard",   "↓",  null, KeyAction.TypeText("↓")),
            Key("s3_star",  "★",  null, KeyAction.TypeText("★")),
            Key("s3_heart", "♥",  null, KeyAction.TypeText("♥")),
            Key("s3_music", "♪",  null, KeyAction.TypeText("♪")),
            Key("s3_check", "✓",  null, KeyAction.TypeText("✓")),
            Key("s3_cross", "✗",  null, KeyAction.TypeText("✗"))
        ),
        bottomRow("◀1")
    )
}
