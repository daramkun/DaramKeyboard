package com.daram.keyboard.prediction

import android.content.Context
import com.daram.nutcracker.prediction.InputLanguage
import com.daram.nutcracker.prediction.UserLearningDelegate
import com.daram.nutcracker.prediction.UserWordEntry

/**
 * SharedPreferences 기반 UserLearningDelegate 구현.
 * 각 단어를 tab-separated 포맷(word\tlanguage\tscore\tuseCount\tlastUsedMs)으로 직렬화해 저장.
 */
class SharedPrefsUserLearningDelegate(context: Context) : UserLearningDelegate {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadUserWords(): List<UserWordEntry> {
        val raw = prefs.getStringSet(KEY_WORDS, emptySet()) ?: return emptyList()
        return raw.mapNotNull { deserialize(it) }
    }

    override fun saveUserWord(entry: UserWordEntry) {
        val existing = prefs.getStringSet(KEY_WORDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        // 같은 단어가 있으면 제거 후 새 항목 삽입
        existing.removeAll { it.startsWith(entry.word + TAB) }
        existing.add(serialize(entry))
        prefs.edit().putStringSet(KEY_WORDS, existing).apply()
    }

    override fun decayAndCleanup(minScore: Float) {
        val existing = prefs.getStringSet(KEY_WORDS, emptySet())?.toMutableSet() ?: return
        val kept = existing.filter { raw ->
            val entry = deserialize(raw) ?: return@filter false
            entry.score >= minScore
        }.toSet()
        if (kept.size != existing.size) {
            prefs.edit().putStringSet(KEY_WORDS, kept).apply()
        }
    }

    private fun serialize(entry: UserWordEntry): String {
        val lang = when (entry.language) {
            InputLanguage.KOREAN -> "ko"
            InputLanguage.ENGLISH -> "en"
            else -> "other"
        }
        return "${entry.word}$TAB$lang$TAB${entry.score}$TAB${entry.useCount}$TAB${entry.lastUsedMs}"
    }

    private fun deserialize(raw: String): UserWordEntry? {
        val parts = raw.split(TAB)
        if (parts.size < 5) return null
        return try {
            val language = when (parts[1]) {
                "ko" -> InputLanguage.KOREAN
                "en" -> InputLanguage.ENGLISH
                else -> InputLanguage.OTHER
            }
            UserWordEntry(
                word = parts[0],
                language = language,
                score = parts[2].toFloat(),
                useCount = parts[3].toInt(),
                lastUsedMs = parts[4].toLong()
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "daram_keyboard_user_words"
        private const val KEY_WORDS = "words"
        private const val TAB = "\t"
    }
}
