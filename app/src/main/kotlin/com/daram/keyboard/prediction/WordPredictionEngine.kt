package com.daram.keyboard.prediction

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.daram.nutcracker.SyllableState
import com.daram.nutcracker.prediction.AmbiguityResolver
import com.daram.nutcracker.prediction.BundledDictionary
import com.daram.nutcracker.prediction.DefaultWordPredictor
import com.daram.nutcracker.prediction.InputLanguage
import com.daram.nutcracker.prediction.KeyMapper
import com.daram.nutcracker.prediction.PredictionQuery
import com.daram.nutcracker.prediction.mapper.NaratgeulKeyMapper
import com.daram.nutcracker.prediction.mapper.QwertyKeyMapper
import com.daram.nutcracker.prediction.trie.TrieDictionary

class WordPredictionEngine(private val context: Context) {

    private val koreanDict = TrieDictionary(InputLanguage.KOREAN)
    private val englishDict = TrieDictionary(InputLanguage.ENGLISH)
    private val learningDelegate = SharedPrefsUserLearningDelegate(context)
    private val predictor = DefaultWordPredictor(listOf(koreanDict, englishDict), learningDelegate)

    private val naratgeulMapper = NaratgeulKeyMapper()
    private val qwertyMapper = QwertyKeyMapper()

    /** 나랏글 물리 키 char ('1'~'0') → keyId 변환 맵 */
    private val naratgeulPhysicalToKeyId = mapOf(
        '1' to "ga", '2' to "na", '3' to "ao",
        '4' to "ra", '5' to "ma", '6' to "ou",
        '7' to "sa", '8' to "eo", '9' to "i",
        '0' to "eu"
    )

    private var lastConfirmedWord = ""
    private var isReady = false

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * 비동기 초기화: nutcracker 번들 사전을 Trie에 반영.
     * @param onReady 준비 완료 시 Main 스레드에서 호출되는 콜백
     */
    fun init(onReady: () -> Unit = {}) {
        scope.launch {
            koreanDict.initialize(BundledDictionary.getWords(InputLanguage.KOREAN))
            englishDict.initialize(BundledDictionary.getWords(InputLanguage.ENGLISH))

            withContext(Dispatchers.Main) {
                isReady = true
                onReady()
            }
        }
    }

    /**
     * 현재 입력 상태로 후보 단어를 최대 3개 반환.
     *
     * @param committedText 확정된 텍스트 (조합 중인 음절 제외)
     * @param composingState 현재 FSM 상태 (HangulInputEngine.syllableState)
     * @param composingText 조합 중인 음절 문자열
     * @param language 입력 언어
     */
    fun getSuggestions(
        committedText: String,
        composingState: SyllableState = SyllableState(),
        composingText: String = "",
        language: InputLanguage = InputLanguage.KOREAN
    ): List<String> {
        if (!isReady || committedText.isEmpty() && composingText.isEmpty()) return emptyList()
        val query = PredictionQuery(
            committedText = committedText,
            composingState = composingState,
            composingText = composingText,
            language = language,
            maxResults = 10
        )
        return predictor.predict(query).map { it.word }.take(3)
    }

    /**
     * 사용자가 예측 후보를 선택했을 때 호출.
     */
    fun onCandidateSelected(word: String, language: InputLanguage = InputLanguage.KOREAN) {
        if (word.isBlank()) return
        predictor.onCandidateSelected(word, language, lastConfirmedWord)
        lastConfirmedWord = word
    }

    /**
     * 사용자가 직접 타이핑해서 단어를 확정했을 때 호출 (스페이스/엔터).
     */
    fun onWordCommitted(word: String, language: InputLanguage = InputLanguage.KOREAN) {
        if (word.isBlank()) return
        predictor.onWordCommitted(word, language, lastConfirmedWord)
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
     * 한글 자판에서 다음에 눌릴 가능성이 높은 키의 히트 영역 확장 힌트를 반환.
     *
     * @param committedText 확정된 텍스트 (조합 중인 음절 제외)
     * @param syllableState 현재 HangulInputEngine의 SyllableState
     * @param composingText 조합 중인 음절 문자열
     * @param keyMapper 현재 자판의 KeyMapper
     * @param ambiguityResolver 비결정적 자판(천지인/SkyII 등)의 AmbiguityResolver; null이면 미확정 없음
     * @return keyId → 예측 가중치 (0.0 ~ 1.0)
     */
    fun getNextKeyHintsForHangul(
        committedText: String,
        syllableState: SyllableState,
        composingText: String,
        keyMapper: KeyMapper,
        ambiguityResolver: AmbiguityResolver? = null
    ): Map<String, Float> {
        if (!isReady || committedText.isEmpty() && composingText.isEmpty()) return emptyMap()
        val pendingJamos = ambiguityResolver?.pendingJamos(syllableState) ?: emptyList()
        val query = PredictionQuery(
            committedText = committedText,
            composingState = syllableState,
            composingText = composingText,
            language = InputLanguage.KOREAN,
            pendingAmbiguous = pendingJamos.isNotEmpty(),
            candidateJamos = pendingJamos,
            maxResults = 5
        )
        val candidates = predictor.predict(query)
        if (candidates.isEmpty()) return emptyMap()

        val hints = predictor.nextKeyHints(candidates, keyMapper)
        return if (keyMapper.layoutName == naratgeulMapper.layoutName) {
            // 나랏글: 물리키 char('1'~'0') → keyId 변환 필요
            hints.keyHints.mapNotNull { (char, weight) ->
                naratgeulPhysicalToKeyId[char]?.let { it to weight }
            }.toMap()
        } else {
            // 두벌식/천지인 등: 물리키 char == keyId (소문자 알파벳 또는 숫자)
            hints.keyHints.mapKeys { (char, _) -> char.toString() }
        }
    }

    /**
     * QWERTY 모드에서 다음에 눌릴 가능성이 높은 키의 히트 영역 확장 힌트를 반환.
     *
     * @param currentInput 현재 타이핑 중인 접두사 (committed + composing)
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
        return hints.keyHints.mapKeys { (char, _) -> char.toString() }
    }
}
