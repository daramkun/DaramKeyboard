package com.daram.keyboard.prediction

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordPredictionEngine(private val context: Context) {
    private val trie = Trie()
    private val userLearning = UserLearningModel(context)

    private var lastConfirmedWord = ""
    private var isReady = false

    private val scope = CoroutineScope(Dispatchers.IO)

    private val naratgulPredictor = NaratgulNextKeyPredictor()
    private val qwertyPredictor = QwertyNextKeyPredictor()

    /**
     * 비동기 초기화: 사전 로드 → 사용자 학습 데이터 로드 → Trie 반영.
     * @param onReady 준비 완료 시 Main 스레드에서 호출되는 콜백
     */
    fun init(onReady: () -> Unit = {}) {
        scope.launch {
            // 1. 기본 사전 로드
            loadDictionary()
            // 2. 사용자 학습 데이터 로드 & Trie에 반영
            userLearning.loadBlocking()
            userLearning.applyToTrie(trie)

            withContext(Dispatchers.Main) {
                isReady = true
                onReady()
            }
        }
    }

    private fun loadDictionary() {
        try {
            context.assets.open("dictionary_ko.txt").bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val word = line.trim()
                    if (word.isNotEmpty()) trie.insert(word, 1)
                }
            }
        } catch (_: Exception) { }
    }

    /**
     * 현재 입력 중인 접두사로 후보 단어를 최대 3개 반환.
     */
    fun getSuggestions(currentInput: String): List<String> {
        if (!isReady || currentInput.isEmpty()) return emptyList()

        var candidates = trie.search(currentInput, maxResults = 10)

        // 사용자 bigram 패턴으로 재정렬
        if (lastConfirmedWord.isNotEmpty()) {
            candidates = userLearning.rerankWithBigram(lastConfirmedWord, candidates)
        }

        return candidates.map { it.first }.take(3)
    }

    /**
     * 사용자가 단어를 확정(스페이스/엔터/후보 탭)했을 때 호출.
     */
    fun onWordConfirmed(word: String) {
        if (word.isBlank()) return
        userLearning.recordWord(lastConfirmedWord, word)
        lastConfirmedWord = word
    }

    /**
     * 입력 필드 포커스가 바뀌거나 입력이 취소될 때 컨텍스트 초기화.
     */
    fun resetContext() {
        lastConfirmedWord = ""
    }

    fun isReady(): Boolean = isReady

    /**
     * 나랏글 모드에서 다음에 눌릴 가능성이 높은 키의 히트 영역 확장 힌트를 반환.
     *
     * @param currentInput 현재 입력 중인 단어 전체 (확정 + 조합 중)
     * @param committedLength 확정된 음절 수
     * @param composerState 현재 HangulComposer 조합 상태
     * @return keyId → 예측 가중치 (0.0 ~ 1.0)
     */
    fun getNextKeyHintsForNaratgul(
        currentInput: String,
        committedLength: Int,
        composerState: NaratgulNextKeyPredictor.ComposerState
    ): Map<String, Float> {
        if (!isReady || currentInput.isEmpty()) return emptyMap()
        // Trie는 완성형 한글만 저장하므로, 조합 중인 낱자가 붙은 currentInput 대신
        // 확정된 음절 수(committedLength)까지의 부분 문자열로 검색
        val searchPrefix = currentInput.take(committedLength).ifEmpty { currentInput }
        val candidates = getSuggestions(searchPrefix)
        return naratgulPredictor.predict(candidates, committedLength, composerState)
    }

    /**
     * QWERTY 모드에서 다음에 눌릴 가능성이 높은 키의 히트 영역 확장 힌트를 반환.
     *
     * @param currentInput 현재 타이핑 중인 접두사
     * @return keyId → 예측 가중치 (0.0 ~ 1.0)
     */
    fun getNextKeyHintsForQwerty(currentInput: String): Map<String, Float> {
        if (!isReady || currentInput.isEmpty()) return emptyMap()
        val candidates = getSuggestions(currentInput)
        return qwertyPredictor.predict(candidates, currentInput.length)
    }
}
