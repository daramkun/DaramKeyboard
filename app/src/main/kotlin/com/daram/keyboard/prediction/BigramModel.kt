package com.daram.keyboard.prediction

/**
 * Bigram 확률 모델.
 * P(다음단어 | 이전단어) 기반으로 후보 단어 목록을 재정렬한다.
 */
class BigramModel {
    // Map<이전단어, Map<다음단어, 빈도>>
    private val counts = HashMap<String, HashMap<String, Int>>()

    fun record(prevWord: String, nextWord: String) {
        counts.getOrPut(prevWord) { HashMap() }
            .merge(nextWord, 1, Int::plus)
    }

    /**
     * 후보 단어 목록을 bigram 확률 기반으로 재정렬.
     * bigram 정보가 없는 후보는 원래 순서 유지.
     */
    fun rerank(prevWord: String, candidates: List<Pair<String, Int>>): List<Pair<String, Int>> {
        val bigramMap = counts[prevWord] ?: return candidates
        val total = bigramMap.values.sum().toFloat()
        return candidates.sortedByDescending { (word, freq) ->
            val bigramScore = (bigramMap[word] ?: 0) / total
            // trie 빈도와 bigram 확률을 혼합 (bigram 가중치 높게)
            freq * 0.3f + bigramScore * 100f * 0.7f
        }
    }

    fun hasBigramFor(prevWord: String): Boolean = counts.containsKey(prevWord)
}
