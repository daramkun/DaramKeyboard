package com.daram.keyboard.settings

import android.content.Context
import androidx.preference.PreferenceManager as AndroidPreferenceManager
import com.daram.keyboard.model.KoreanKeyboardType

object PreferenceManager {
    private const val KEY_HAPTIC = "haptic_enabled"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_THEME = "theme"
    private const val KEY_WORD_PREDICTION = "word_prediction"
    private const val KEY_SHOW_HIT_TARGETS = "show_hit_targets"
    private const val KEY_KOREAN_KEYBOARD_TYPE = "korean_keyboard_type"

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

    fun getKoreanKeyboardType(context: Context): KoreanKeyboardType {
        val prefValue = AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_KOREAN_KEYBOARD_TYPE, KoreanKeyboardType.NARATGEUL.prefValue)
            ?: KoreanKeyboardType.NARATGEUL.prefValue
        return KoreanKeyboardType.entries.firstOrNull { it.prefValue == prefValue }
            ?: KoreanKeyboardType.NARATGEUL
    }

    fun setKoreanKeyboardType(context: Context, type: KoreanKeyboardType) {
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(KEY_KOREAN_KEYBOARD_TYPE, type.prefValue)
            .apply()
    }

    /** 다음 한국어 자판 타입으로 순환 */
    fun cycleKoreanKeyboardType(context: Context): KoreanKeyboardType {
        val types = KoreanKeyboardType.entries
        val current = getKoreanKeyboardType(context)
        val next = types[(types.indexOf(current) + 1) % types.size]
        setKoreanKeyboardType(context, next)
        return next
    }
}
