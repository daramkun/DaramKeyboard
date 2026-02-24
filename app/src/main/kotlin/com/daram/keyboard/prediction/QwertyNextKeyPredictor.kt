package com.daram.keyboard.prediction

/**
 * QWERTY 영어 입력 상태에서 다음에 눌릴 가능성이 높은 키를 예측.
 *
 * 동작 방식:
 * 1. WordPredictionEngine의 현재 후보 단어 목록을 받아,
 *    현재 입력된 접두사 길이 다음에 오는 글자를 추출한다.
 * 2. 글자별 점수를 합산하여 QWERTY 키 ID로 매핑, 정규화하여 반환.
 *
 * QWERTY 키 ID 매핑:
 *   알파벳 소문자 → 키 ID가 해당 알파벳 문자열 (e.g. 'a' → "a")
 *   대/소문자 모두 동일 키에 매핑.
 */
class QwertyNextKeyPredictor {

    /**
     * 현재 예측 후보 단어들과 현재까지 입력된 글자 수를 받아,
     * 다음에 눌릴 가능성이 높은 키 ID별 가중치를 반환.
     *
     * @param candidates WordPredictionEngine.getSuggestions()의 결과
     * @param typedLength 현재까지 타이핑한 접두사 길이
     * @return keyId → 예측 가중치 (0.0 ~ 1.0)
     */
    fun predict(candidates: List<String>, typedLength: Int): Map<String, Float> {
        if (candidates.isEmpty()) return emptyMap()

        val candidateScores = listOf(1.0f, 0.6f, 0.3f)
        val charScores = mutableMapOf<Char, Float>()

        for ((idx, word) in candidates.withIndex()) {
            val score = candidateScores.getOrElse(idx) { 0.2f }
            if (typedLength >= word.length) continue

            // 다음 글자 (가중치 1.0)
            val nextChar = word[typedLength].lowercaseChar()
            if (nextChar.isLetter()) {
                charScores.merge(nextChar, score * 1.0f, Float::plus)
            }
            // 그 다음 글자 (가중치 0.5)
            if (typedLength + 1 < word.length) {
                val secondChar = word[typedLength + 1].lowercaseChar()
                if (secondChar.isLetter()) {
                    charScores.merge(secondChar, score * 0.5f, Float::plus)
                }
            }
        }

        if (charScores.isEmpty()) return emptyMap()

        // 알파벳 → QWERTY 키 ID (소문자 = 키 ID)
        val keyScores = mutableMapOf<String, Float>()
        for ((char, score) in charScores) {
            keyScores[char.toString()] = score
        }

        // 최댓값으로 정규화
        val maxScore = keyScores.values.maxOrNull() ?: return emptyMap()
        return keyScores.mapValues { (_, v) -> v / maxScore }
    }
}
