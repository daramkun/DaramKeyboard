package com.daram.keyboard.theme

import android.graphics.Color

data class KeyboardTheme(
    val backgroundColor: Int,
    val keyNormalBackground: Int,
    val keyFunctionBackground: Int,
    val keyPressedBackground: Int,
    val keyLabelColor: Int,
    val keySecondaryLabelColor: Int,
    val dividerColor: Int,
    val keyLabelSizeSp: Float = 20f,
    val keySecondaryLabelSizeSp: Float = 11f
)

object KeyboardThemes {
    val LIGHT = KeyboardTheme(
        backgroundColor = Color.parseColor("#E8E8E8"),
        keyNormalBackground = Color.WHITE,
        keyFunctionBackground = Color.parseColor("#D0D0D0"),
        keyPressedBackground = Color.parseColor("#BBBBBB"),
        keyLabelColor = Color.BLACK,
        keySecondaryLabelColor = Color.parseColor("#888888"),
        dividerColor = Color.parseColor("#CCCCCC")
    )

    val DARK = KeyboardTheme(
        backgroundColor = Color.parseColor("#1A1A1A"),
        keyNormalBackground = Color.parseColor("#3A3A3A"),
        keyFunctionBackground = Color.parseColor("#2A2A2A"),
        keyPressedBackground = Color.parseColor("#555555"),
        keyLabelColor = Color.WHITE,
        keySecondaryLabelColor = Color.parseColor("#AAAAAA"),
        dividerColor = Color.parseColor("#222222")
    )
}
