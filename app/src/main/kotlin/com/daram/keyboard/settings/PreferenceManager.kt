package com.daram.keyboard.settings

import android.content.Context
import androidx.preference.PreferenceManager as AndroidPreferenceManager

object PreferenceManager {
    private const val KEY_HAPTIC = "haptic_enabled"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_THEME = "theme"
    private const val KEY_WORD_PREDICTION = "word_prediction"
    private const val KEY_SHOW_HIT_TARGETS = "show_hit_targets"

    fun isHapticEnabled(context: Context): Boolean =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_HAPTIC, true)

    fun isSoundEnabled(context: Context): Boolean =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SOUND, false)

    fun getTheme(context: Context): String =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_THEME, "auto") ?: "auto"

    fun isWordPredictionEnabled(context: Context): Boolean =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_WORD_PREDICTION, true)

    fun isShowHitTargetsEnabled(context: Context): Boolean =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SHOW_HIT_TARGETS, false)
}
