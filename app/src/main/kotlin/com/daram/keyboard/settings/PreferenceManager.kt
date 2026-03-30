package com.daram.keyboard.settings

import android.content.Context
import androidx.preference.PreferenceManager as AndroidPreferenceManager
import com.daram.keyboard.model.KoreanKeyboardType

object PreferenceManager {
    private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_THEME = "theme"
    private const val KEY_WORD_PREDICTION = "word_prediction"
    private const val KEY_SHOW_HIT_TARGETS = "show_hit_targets"
    private const val KEY_KOREAN_KEYBOARD_TYPE = "korean_keyboard_type"
    private const val KEY_KEYBOARD_HEIGHT = "keyboard_height"
    private const val KEY_KEY_POPUP = "key_popup"
    private const val KEY_EMOJI_SKIN_TONE = "emoji_skin_tone"
    private const val KEY_AUTO_PERIOD_DOUBLE_SPACE = "auto_period_double_space"
    private const val KEY_AUTO_COMPOSITION_TIMEOUT = "auto_composition_timeout"
    private const val KEY_ORIENTATION_LAYOUT = "orientation_layout"

    /** 진동 켜짐 여부: intensity가 "off"가 아니면 켜짐 */
    fun isHapticEnabled(context: Context): Boolean =
        getHapticIntensity(context) != "off"

    /** 햅틱 강도: "off" / "weak" / "medium" / "strong" */
    fun getHapticIntensity(context: Context): String =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_HAPTIC_INTENSITY, "medium") ?: "medium"

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

    /** 키보드 높이: "small"(240dp) / "medium"(280dp) / "large"(320dp) */
    fun getKeyboardHeight(context: Context): String =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_KEYBOARD_HEIGHT, "medium") ?: "medium"

    fun isKeyPopupEnabled(context: Context): Boolean =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_KEY_POPUP, true)

    /** 이모지 피부색: "none" / "light" / "medium_light" / "medium" / "medium_dark" / "dark" */
    fun getEmojiSkinTone(context: Context): String =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_EMOJI_SKIN_TONE, "none") ?: "none"

    fun isAutoPeriodOnDoubleSpaceEnabled(context: Context): Boolean =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_AUTO_PERIOD_DOUBLE_SPACE, false)

    /** 자동 조합 완성 지연(ms). 0이면 비활성 */
    fun getAutoCompositionTimeout(context: Context): Int =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_AUTO_COMPOSITION_TIMEOUT, "0")?.toIntOrNull() ?: 0

    /** 화면 방향에 따라 기본 레이아웃을 자동 변경할지 여부 */
    fun isOrientationLayoutEnabled(context: Context): Boolean =
        AndroidPreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_ORIENTATION_LAYOUT, false)

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
