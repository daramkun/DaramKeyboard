package com.daram.keyboard.prediction

import com.daram.keyboard.input.HangulComposer

/**
 * 나랏글 한글 입력 상태에서 다음에 눌릴 가능성이 높은 키(자소)를 예측.
 *
 * 동작 방식:
 * 1. WordPredictionEngine의 현재 후보 단어 목록을 받아, 각 후보에서
 *    현재 조합 위치 다음에 오는 자소(자모 분해)를 추출한다.
 * 2. 자소별 점수를 합산하여 정규화한다 (0.0 ~ 1.0).
 * 3. 나랏글 레이아웃의 키 ID와 자소를 매핑하여 keyId → 가중치를 반환한다.
 *
 * 나랏글 키 자소 매핑:
 *   "ga"  → ㄱ       "na"  → ㄴ       "ao"  → ㅏ, ㅓ (ㅏ 키)
 *   "ra"  → ㄹ       "ma"  → ㅁ       "ou"  → ㅗ, ㅜ (ㅗ 키)
 *   "sa"  → ㅅ       "eo"  → ㅇ       "i"   → ㅣ
 */
class NaratgulNextKeyPredictor {

    // 나랏글 키 ID → 해당 키로 입력 가능한 자소 목록
    private val keyJamoMap: Map<String, List<Char>> = mapOf(
        "ga" to listOf('ㄱ', 'ㄲ', 'ㅋ'),   // ㄱ 키: ㄱ, 획추가→ㅋ, 쌍자음→ㄲ
        "na" to listOf('ㄴ', 'ㄷ', 'ㅌ'),   // ㄴ 키: ㄴ, 획추가→ㄷ→ㅌ
        "ao" to listOf('ㅏ', 'ㅓ'),         // ㅏㅓ 키
        "ra" to listOf('ㄹ'),
        "ma" to listOf('ㅁ', 'ㅂ', 'ㅍ'),   // ㅁ 키: ㅁ, 획추가→ㅂ→ㅍ
        "ou" to listOf('ㅗ', 'ㅜ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ'), // ㅗㅜ 키 + 복합모음
        "sa" to listOf('ㅅ', 'ㅈ', 'ㅊ', 'ㅆ', 'ㅉ'), // ㅅ 키: ㅅ, 획추가→ㅈ→ㅊ, 쌍자음→ㅆ→ㅉ
        "eo" to listOf('ㅇ', 'ㅎ'),         // ㅇ 키: ㅇ, 획추가→ㅎ
        "i"  to listOf('ㅣ', 'ㅡ', 'ㅢ')    // ㅣ 키: ㅣ, 복합모음 ㅡ+ㅣ=ㅢ
    )

    // 자소 → 키 ID 역방향 맵 (자소 하나가 여러 키를 가리킬 수 있으나 나랏글에선 1:1)
    private val jamoToKeyId: Map<Char, String> by lazy {
        val map = mutableMapOf<Char, String>()
        for ((keyId, jamoList) in keyJamoMap) {
            for (jamo in jamoList) {
                // 이미 등록된 자소는 첫 번째 키 우선 (더 기본적인 키)
                if (!map.containsKey(jamo)) map[jamo] = keyId
            }
        }
        map
    }

    /**
     * 현재 예측 후보 단어들과 현재까지 확정된 글자 수를 받아,
     * 다음에 눌릴 가능성이 높은 키 ID별 가중치를 반환.
     *
     * @param candidates WordPredictionEngine.getSuggestions()의 결과 (최대 3개)
     * @param committedLength 이미 확정된 한글 글자 수 (currentWordBuffer 기준)
     * @param composerState 현재 조합 중인 HangulComposer 상태 (자소 레벨 위치 계산에 사용)
     * @return keyId → 예측 가중치 (0.0 ~ 1.0)
     */
    fun predict(
        candidates: List<String>,
        committedLength: Int,
        composerState: ComposerState
    ): Map<String, Float> {
        if (candidates.isEmpty()) return emptyMap()

        // 후보별 점수 (1등 → 1.0, 2등 → 0.6, 3등 → 0.3)
        val candidateScores = listOf(1.0f, 0.6f, 0.3f)

        // 자소별 누적 점수
        val jamoScores = mutableMapOf<Char, Float>()

        for ((idx, word) in candidates.withIndex()) {
            val score = candidateScores.getOrElse(idx) { 0.2f }
            // 현재 입력 위치 이후 자소들을 분해하여 첫 번째 자소를 추출
            val nextJamos = extractNextJamos(word, committedLength, composerState)
            for ((jamo, jamoScore) in nextJamos) {
                jamoScores.merge(jamo, score * jamoScore, Float::plus)
            }
        }

        if (jamoScores.isEmpty()) return emptyMap()

        // 키 ID별로 합산 (한 키에 여러 자소가 매핑된 경우 최댓값 사용)
        val keyScores = mutableMapOf<String, Float>()
        for ((jamo, score) in jamoScores) {
            val keyId = jamoToKeyId[jamo] ?: continue
            keyScores[keyId] = maxOf(keyScores.getOrDefault(keyId, 0f), score)
        }

        // 최댓값으로 정규화 (0.0 ~ 1.0)
        val maxScore = keyScores.values.maxOrNull() ?: return emptyMap()
        return keyScores.mapValues { (_, v) -> v / maxScore }
    }

    /**
     * 예측 단어에서 현재 입력 위치 이후에 오는 자소들을 추출.
     *
     * 입력 위치 결정:
     * - committedLength: 확정된 한글 음절 수
     * - composerState.syllableOffset: 조합 중인 음절 내에서 이미 입력된 자소 수
     *
     * 반환값: 다음에 입력될 자소 → 해당 자소가 "즉시 다음" 자소일 확률
     *   (첫 번째 자소 = 1.0, 두 번째 = 0.5, 세 번째 이후는 무시)
     */
    private fun extractNextJamos(
        word: String,
        committedLength: Int,
        composerState: ComposerState
    ): Map<Char, Float> {
        // 단어를 자소로 분해
        val jamos = decomposeToJamos(word)
        if (jamos.isEmpty()) return emptyMap()

        // 이미 입력한 자소 수 = 확정 음절의 자소 수 + 현재 조합 중인 자소 수
        val alreadyInputJamoCount = countJamosInSyllables(word, committedLength) +
                composerState.inputtedJamoCount

        if (alreadyInputJamoCount >= jamos.size) return emptyMap()

        val result = mutableMapOf<Char, Float>()
        // 다음 자소 (가중치 1.0)
        result[jamos[alreadyInputJamoCount]] = 1.0f
        // 그 다음 자소 (가중치 0.5 — 조합 흐름 예측)
        if (alreadyInputJamoCount + 1 < jamos.size) {
            val secondJamo = jamos[alreadyInputJamoCount + 1]
            result.merge(secondJamo, 0.5f, ::maxOf)
        }
        return result
    }

    /**
     * 한글 단어를 자소 단위로 분해.
     * 완성형 음절 → 초성, 중성, 종성(있을 경우) 순으로 분해.
     * 낱자는 그대로 포함.
     */
    private fun decomposeToJamos(word: String): List<Char> {
        val result = mutableListOf<Char>()
        for (c in word) {
            val code = c.code
            when {
                code in 0xAC00..0xD7A3 -> {
                    val offset = code - 0xAC00
                    val choIdx = offset / 28 / 21
                    val jungIdx = (offset / 28) % 21
                    val jongIdx = offset % 28
                    result.add(CHOSEONG[choIdx])
                    result.add(JUNGSEONG[jungIdx])
                    if (jongIdx != 0) result.add(JONGSEONG[jongIdx])
                }
                code in 0x3131..0x3163 -> result.add(c)
                else -> { /* 한글 외 문자는 자소 분해 불가 */ }
            }
        }
        return result
    }

    /**
     * 단어의 앞 n개 음절에 해당하는 자소 수를 반환.
     * 음절 수 n이 단어 길이보다 크면 전체 자소 수를 반환.
     */
    private fun countJamosInSyllables(word: String, syllableCount: Int): Int {
        var count = 0
        for (i in 0 until minOf(syllableCount, word.length)) {
            val code = word[i].code
            when {
                code in 0xAC00..0xD7A3 -> {
                    val jongIdx = (code - 0xAC00) % 28
                    count += if (jongIdx != 0) 3 else 2
                }
                code in 0x3131..0x3163 -> count += 1
                else -> { }
            }
        }
        return count
    }

    /**
     * 현재 HangulComposer의 조합 상태를 나타내는 데이터 클래스.
     * KeyboardView에서 생성하여 predict()에 전달.
     */
    data class ComposerState(
        /** 현재 조합 중인 음절 내에서 이미 입력된 자소 수 (0 ~ 3) */
        val inputtedJamoCount: Int
    ) {
        companion object {
            val EMPTY = ComposerState(0)

            /**
             * HangulComposer의 조합 텍스트로부터 현재 입력된 자소 수를 추출.
             * composingText가 완성형 음절이면 자소 분해, 낱자면 1.
             */
            fun fromComposingText(composingText: String): ComposerState {
                if (composingText.isEmpty()) return EMPTY
                val c = composingText.last()
                val code = c.code
                val count = when {
                    code in 0xAC00..0xD7A3 -> {
                        val jongIdx = (code - 0xAC00) % 28
                        if (jongIdx != 0) 3 else 2
                    }
                    code in 0x3131..0x3163 -> 1
                    else -> 0
                }
                return ComposerState(count)
            }
        }
    }

    companion object {
        private val CHOSEONG = charArrayOf(
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
        )
        private val JUNGSEONG = charArrayOf(
            'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ',
            'ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ'
        )
        private val JONGSEONG = charArrayOf(
            '\u0000','ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ',
            'ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
        )
    }
}
