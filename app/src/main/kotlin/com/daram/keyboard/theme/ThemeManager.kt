package com.daram.keyboard.theme

import android.content.Context
import android.content.res.Configuration

object ThemeManager {
    fun resolveTheme(context: Context, themePref: String): KeyboardTheme {
        return when (themePref) {
            "light" -> KeyboardThemes.LIGHT
            "dark" -> KeyboardThemes.DARK
            else -> {
                val nightModeFlags = context.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    KeyboardThemes.DARK
                } else {
                    KeyboardThemes.LIGHT
                }
            }
        }
    }
}
