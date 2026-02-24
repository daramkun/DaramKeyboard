package com.daram.keyboard.prediction

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 사용자 입력 패턴 학습 모델.
 *
 * 사용자가 확정한 단어의 unigram 빈도와 bigram 패턴을
 * 로컬 JSON 파일에 저장하여 앱 재시작 후에도 유지.
 *
 * 구조:
 *   {
 *     "unigram": { "단어": 빈도, ... },
 *     "bigram": { "이전단어": { "다음단어": 빈도, ... }, ... }
 *   }
 */
class UserLearningModel(private val context: Context) {
    private val fileName = "user_learning.json"
    private val scope = CoroutineScope(Dispatchers.IO)

    // 메모리 캐시
    private val unigram = HashMap<String, Int>()
    private val bigram = HashMap<String, HashMap<String, Int>>()

    private var loaded = false

    fun loadAsync(onLoaded: () -> Unit) {
        scope.launch {
            load()
            withContext(Dispatchers.Main) { onLoaded() }
        }
    }

    /** IO 스레드에서 동기 호출용. WordPredictionEngine 초기화 시 사용. */
    fun loadBlocking() = load()

    private fun load() {
        try {
            val file = context.getFileStreamPath(fileName)
            if (!file.exists()) return
            val json = JSONObject(file.readText())

            val unigramJson = json.optJSONObject("unigram") ?: return
            for (key in unigramJson.keys()) {
                unigram[key] = unigramJson.getInt(key)
            }

            val bigramJson = json.optJSONObject("bigram") ?: return
            for (prev in bigramJson.keys()) {
                val nextMap = HashMap<String, Int>()
                val nextJson = bigramJson.getJSONObject(prev)
                for (next in nextJson.keys()) {
                    nextMap[next] = nextJson.getInt(next)
                }
                bigram[prev] = nextMap
            }
        } catch (_: Exception) {
            // 손상된 파일은 무시하고 새로 시작
        } finally {
            loaded = true
        }
    }

    private fun saveAsync() {
        scope.launch { save() }
    }

    private fun save() {
        try {
            val unigramJson = JSONObject()
            for ((k, v) in unigram) unigramJson.put(k, v)

            val bigramJson = JSONObject()
            for ((prev, nextMap) in bigram) {
                val nextJson = JSONObject()
                for ((next, cnt) in nextMap) nextJson.put(next, cnt)
                bigramJson.put(prev, nextJson)
            }

            val json = JSONObject()
            json.put("unigram", unigramJson)
            json.put("bigram", bigramJson)

            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(json.toString().toByteArray())
            }
        } catch (_: Exception) { }
    }

    /**
     * 사용자가 단어를 확정했을 때 호출.
     * @param prevWord 직전에 확정된 단어 (bigram 학습용)
     * @param word 현재 확정된 단어
     */
    fun recordWord(prevWord: String, word: String) {
        if (!loaded) return
        unigram.merge(word, 1, Int::plus)
        if (prevWord.isNotEmpty()) {
            bigram.getOrPut(prevWord) { HashMap() }.merge(word, 1, Int::plus)
        }
        saveAsync()
    }

    /**
     * 사용자 학습 데이터를 Trie에 반영.
     * 기존 Trie의 빈도를 사용자 빈도로 보강한다.
     */
    fun applyToTrie(trie: Trie) {
        for ((word, freq) in unigram) {
            trie.insert(word, freq * 5)  // 사용자 입력은 가중치 높게
        }
    }

    /**
     * bigram 기반으로 후보 재정렬.
     */
    fun rerankWithBigram(prevWord: String, candidates: List<Pair<String, Int>>): List<Pair<String, Int>> {
        val bigramMap = bigram[prevWord] ?: return candidates
        val total = bigramMap.values.sum().toFloat()
        return candidates.sortedByDescending { (word, freq) ->
            val bigramScore = (bigramMap[word] ?: 0) / total
            freq * 0.3f + bigramScore * 100f * 0.7f
        }
    }

    fun isLoaded(): Boolean = loaded
}
