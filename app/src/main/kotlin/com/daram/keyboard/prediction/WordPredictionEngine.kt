package com.daram.keyboard.prediction

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import `in`.daram.nutcracker.SyllableState
import `in`.daram.nutcracker.toComposingString
import `in`.daram.nutcracker.prediction.DefaultWordPredictor
import `in`.daram.nutcracker.prediction.InputLanguage
import `in`.daram.nutcracker.prediction.PredictionQuery
import `in`.daram.nutcracker.prediction.mapper.NaratgeulKeyMapper
import `in`.daram.nutcracker.prediction.mapper.QwertyKeyMapper
import `in`.daram.nutcracker.prediction.trie.TrieDictionary
import `in`.daram.nutcracker.prediction.WordEntry

class WordPredictionEngine(private val context: Context) {

    private val koreanDict = TrieDictionary(InputLanguage.KOREAN)
    private val englishDict = TrieDictionary(InputLanguage.ENGLISH)
    private val predictor = DefaultWordPredictor(listOf(koreanDict, englishDict))

    private val naratgeulMapper = NaratgeulKeyMapper()
    private val qwertyMapper = QwertyKeyMapper()

    /** 나랏글 물리 키 char → keyId 변환 맵 */
    private val naratgeulCharToKeyId = mapOf(
        'ㄱ' to "ga", 'ㄴ' to "na", 'ㅏ' to "ao",
        'ㄹ' to "ra", 'ㅁ' to "ma", 'ㅗ' to "ou",
        'ㅅ' to "sa", 'ㅇ' to "eo", 'ㅣ' to "i",
        'ㅡ' to "eu"
    )

    private var lastConfirmedWord = ""
    private var isReady = false

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * 비동기 초기화: 한국어/영어 사전 로드 후 Trie에 반영.
     * @param onReady 준비 완료 시 Main 스레드에서 호출되는 콜백
     */
    fun init(onReady: () -> Unit = {}) {
        scope.launch {
            val koEntries = loadDictionary("dictionary_ko.txt")
            koreanDict.initialize(koEntries)
            val enEntries = loadDictionary("dictionary_en.txt")
            if (enEntries.isNotEmpty()) englishDict.initialize(enEntries)

            withContext(Dispatchers.Main) {
                isReady = true
                onReady()
            }
        }
    }

    private fun loadDictionary(fileName: String): List<WordEntry> {
        return try {
            context.assets.open(fileName).bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapIndexed { idx, line ->
                        // "단어 빈도수" 형식 지원, 없으면 역순 인덱스 기반 점수
                        val parts = line.split('\t', ' ')
                        val word = parts[0]
                        val freq = parts.getOrNull(1)?.toIntOrNull() ?: (100_000 - idx).coerceAtLeast(1)
                        WordEntry(word, freq)
                    }
                    .toList()
            }
        } catch (_: Exception) { emptyList() }
    }

    /**
     * 현재 입력 접두사로 후보 단어를 최대 3개 반환.
     */
    fun getSuggestions(currentInput: String): List<String> {
        if (!isReady || currentInput.isEmpty()) return emptyList()
        val query = PredictionQuery(
            committedText = currentInput,
            composingState = SyllableState(),
            composingText = "",
            language = InputLanguage.KOREAN,
            maxResults = 10
        )
        return predictor.predict(query).map { it.word }.take(3)
    }

    /**
     * 사용자가 단어를 확정했을 때 호출.
     */
    fun onWordConfirmed(word: String) {
        if (word.isBlank()) return
        predictor.onWordCommitted(word, InputLanguage.KOREAN, lastConfirmedWord)
        lastConfirmedWord = word
    }

    /**
     * 포커스 변경 시 컨텍스트 초기화.
     */
    fun resetContext() {
        lastConfirmedWord = ""
    }

    fun isReady(): Boolean = isReady

    /**
     * 나랏글 모드에서 다음에 눌릴 가능성이 높은 키의 히트 영역 확장 힌트를 반환.
     *
     * @param committedText 확정된 음절 부분 (조합 중인 자소 제외)
     * @param syllableState 현재 NaratgulInputEngine의 SyllableState
     * @return keyId → 예측 가중치 (0.0 ~ 1.0)
     */
    fun getNextKeyHintsForNaratgul(
        committedText: String,
        syllableState: SyllableState
    ): Map<String, Float> {
        if (!isReady || committedText.isEmpty() && syllableState.toComposingString().isEmpty()) return emptyMap()
        val query = PredictionQuery(
            committedText = committedText,
            composingState = syllableState,
            composingText = syllableState.toComposingString(),
            language = InputLanguage.KOREAN,
            maxResults = 5
        )
        val candidates = predictor.predict(query)
        if (candidates.isEmpty()) return emptyMap()

        val hints = predictor.nextKeyHints(candidates, naratgeulMapper)
        return hints.keyHints
            .mapNotNull { (char, weight) ->
                val keyId = naratgeulCharToKeyId[char] ?: return@mapNotNull null
                keyId to weight
            }
            .toMap()
    }

    /**
     * QWERTY 모드에서 다음에 눌릴 가능성이 높은 키의 히트 영역 확장 힌트를 반환.
     *
     * @param currentInput 현재 타이핑 중인 접두사
     * @return keyId → 예측 가중치 (0.0 ~ 1.0)
     */
    fun getNextKeyHintsForQwerty(currentInput: String): Map<String, Float> {
        if (!isReady || currentInput.isEmpty()) return emptyMap()
        val query = PredictionQuery(
            committedText = currentInput,
            composingState = SyllableState(),
            composingText = "",
            language = InputLanguage.ENGLISH,
            maxResults = 5
        )
        val candidates = predictor.predict(query)
        if (candidates.isEmpty()) return emptyMap()

        val hints = predictor.nextKeyHints(candidates, qwertyMapper)
        // QWERTY: Char는 소문자 알파벳, keyId도 동일
        return hints.keyHints.mapKeys { (char, _) -> char.toString() }
    }
}
