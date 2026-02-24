package com.daram.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.daram.keyboard.feedback.HapticFeedbackManager
import com.daram.keyboard.feedback.SoundFeedbackManager
import com.daram.keyboard.input.NaratgulInputEngine
import com.daram.keyboard.input.QwertyInputEngine
import com.daram.keyboard.layout.EmojiLayout
import com.daram.keyboard.layout.KeyboardLayout
import com.daram.keyboard.layout.NaratgulLayout
import com.daram.keyboard.layout.NumberLayout
import com.daram.keyboard.layout.QwertyLayout
import com.daram.keyboard.layout.QwertySymbolLayout
import com.daram.keyboard.layout.SymbolLayout
import com.daram.keyboard.prediction.NaratgulNextKeyPredictor
import com.daram.keyboard.prediction.WordPredictionEngine
import com.daram.keyboard.settings.PreferenceManager
import com.daram.keyboard.theme.ThemeManager
import com.daram.keyboard.view.CandidateBarView
import com.daram.keyboard.view.EmojiKeyboardView
import com.daram.keyboard.view.KeyboardView

class DaramInputMethodService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var emojiKeyboardView: EmojiKeyboardView
    private lateinit var keyboardContainer: FrameLayout
    private lateinit var candidateBarView: CandidateBarView
    private lateinit var hapticManager: HapticFeedbackManager
    private lateinit var soundManager: SoundFeedbackManager
    private lateinit var predictionEngine: WordPredictionEngine

    private val naratgulEngine = NaratgulInputEngine()
    private val qwertyEngine = QwertyInputEngine()

    private var currentLayout: KeyboardLayout = NaratgulLayout
    private var currentComposingWord = ""
    private var isEmojiMode = false
    // 이모지 패널 복귀 대상 레이아웃
    private var prevLayoutBeforeEmoji: KeyboardLayout = NaratgulLayout

    override fun onCreate() {
        super.onCreate()
        hapticManager = HapticFeedbackManager(this)
        soundManager = SoundFeedbackManager(this).also { it.init() }
        predictionEngine = WordPredictionEngine(this)
        predictionEngine.init()
    }

    // fullscreen 모드를 항상 끔: 활성화 시 시스템이 키보드 위에
    // "완료"/"숨기기" 버튼 오버레이를 그려서 키 영역과 겹침
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val theme = ThemeManager.resolveTheme(this, PreferenceManager.getTheme(this))

        candidateBarView = CandidateBarView(
            context = this,
            theme = theme,
            onCandidateSelected = { word -> onCandidateSelected(word) }
        )

        keyboardView = KeyboardView(
            context = this,
            theme = theme,
            inputEngine = naratgulEngine,
            onLayoutSwitch = { newLayout -> switchLayout(newLayout) },
            onKeyPressed = { performFeedback() },
            onComposingChanged = { composing -> onComposingChanged(composing) },
            onWordCommitted = { word -> onWordCommitted(word) }
        )

        emojiKeyboardView = EmojiKeyboardView(
            context = this,
            theme = theme,
            onEmojiSelected = { emoji ->
                currentInputConnection?.commitText(emoji, 1)
                performFeedback()
            },
            onBackspace = {
                currentInputConnection?.deleteSurroundingText(1, 0)
                performFeedback()
            },
            onBack = {
                showKeyboard()
            }
        )

        keyboardView.inputConnection = currentInputConnection
        keyboardView.post { applyGestureNavPadding() }

        // 키보드/이모지 전환용 컨테이너
        keyboardContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        keyboardContainer.addView(keyboardView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        keyboardContainer.addView(emojiKeyboardView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        emojiKeyboardView.visibility = View.GONE

        val outerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val candidateResId = resources.getIdentifier("candidate_bar_height", "dimen", packageName)
        val candidateHeight = if (candidateResId != 0) resources.getDimensionPixelSize(candidateResId)
                              else dpToPx(44f).toInt()

        outerContainer.addView(candidateBarView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            candidateHeight
        ))
        outerContainer.addView(keyboardContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        return outerContainer
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        naratgulEngine.reset()
        qwertyEngine.reset()
        predictionEngine.resetContext()
        currentComposingWord = ""
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (::keyboardView.isInitialized) {
            keyboardView.inputConnection = currentInputConnection

            val theme = ThemeManager.resolveTheme(this, PreferenceManager.getTheme(this))
            keyboardView.applyTheme(theme)
            candidateBarView.applyTheme(theme)
            if (::emojiKeyboardView.isInitialized) {
                emojiKeyboardView.applyTheme(theme)
            }

            // 서브타입 기반 초기 레이아웃 적용
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            applySubtypeLayout(imm.currentInputMethodSubtype)
            applyGestureNavPadding()
        }
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        if (::keyboardView.isInitialized) {
            applySubtypeLayout(newSubtype)
        }
    }

    private fun applySubtypeLayout(subtype: InputMethodSubtype?) {
        val isEnglish = subtype?.languageTag?.startsWith("en") == true
        val targetLayout = if (isEnglish) QwertyLayout else NaratgulLayout
        if (currentLayout == targetLayout) return
        // 이모지 모드 중이라면 복귀 대상 레이아웃만 변경
        if (isEmojiMode) {
            prevLayoutBeforeEmoji = targetLayout
            return
        }
        currentLayout = targetLayout
        val newEngine = if (isEnglish) qwertyEngine else naratgulEngine
        keyboardView.switchLayout(targetLayout, newEngine)
        keyboardView.inputConnection = currentInputConnection
        naratgulEngine.reset()
        qwertyEngine.reset()
    }

    private fun applyGestureNavPadding() {
        if (!::keyboardView.isInitialized) return
        val decorView = window?.window?.decorView ?: return
        val insets = decorView.rootWindowInsets ?: return
        val navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        val tappableBottom = insets.getInsets(WindowInsets.Type.tappableElement()).bottom
        val systemBottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom
        val bottomPx = maxOf(navBottom, tappableBottom, systemBottom)
        keyboardView.updateGestureNavPadding(bottomPx)
        if (::emojiKeyboardView.isInitialized) {
            emojiKeyboardView.setGestureNavPadding(bottomPx)
            // 이모지 패널 높이를 KeyboardView와 동일하게 맞춤
            // KeyboardView 높이가 아직 0이면 post로 재시도
            val kbHeight = keyboardView.measuredHeight
            if (kbHeight > 0) {
                (emojiKeyboardView.layoutParams as? FrameLayout.LayoutParams)?.height = kbHeight
                emojiKeyboardView.requestLayout()
            } else {
                keyboardView.post {
                    val h = keyboardView.measuredHeight
                    if (h > 0) {
                        (emojiKeyboardView.layoutParams as? FrameLayout.LayoutParams)?.height = h
                        emojiKeyboardView.requestLayout()
                    }
                }
            }
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        naratgulEngine.reset()
        qwertyEngine.reset()
        currentComposingWord = ""
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }

    private fun switchLayout(newLayout: KeyboardLayout) {
        // 이모지 레이아웃은 EmojiKeyboardView로 처리
        if (newLayout is EmojiLayout) {
            showEmojiPanel()
            return
        }

        showKeyboard()
        currentLayout = newLayout
        // QWERTY 계열(QwertyLayout, QwertySymbolLayout)은 영문 엔진, 나머지는 나랏글 엔진
        // 기호는 엔진을 변경하지 않음 (입력 없음)
        val newEngine = when {
            newLayout == QwertyLayout || newLayout is QwertySymbolLayout -> qwertyEngine
            newLayout is SymbolLayout || newLayout == NumberLayout -> null
            else -> naratgulEngine
        }
        keyboardView.switchLayout(newLayout, newEngine)
        keyboardView.inputConnection = currentInputConnection
        performFeedback()
    }

    private fun showEmojiPanel() {
        if (!::emojiKeyboardView.isInitialized || !::keyboardView.isInitialized) return
        prevLayoutBeforeEmoji = currentLayout
        isEmojiMode = true
        // 이모지 패널 높이를 KeyboardView 현재 높이에 맞춤
        val kbHeight = keyboardView.measuredHeight
        if (kbHeight > 0) {
            (emojiKeyboardView.layoutParams as? FrameLayout.LayoutParams)?.height = kbHeight
            emojiKeyboardView.requestLayout()
        }
        keyboardView.visibility = View.GONE
        emojiKeyboardView.visibility = View.VISIBLE
        emojiKeyboardView.selectCategory(0)
        // 뒤로가기 버튼 레이블: 이전 레이아웃이 QWERTY 계열이면 "EN", 나머지는 "한"
        val backLabel = if (currentLayout == QwertyLayout || currentLayout is QwertySymbolLayout) "EN" else "한"
        emojiKeyboardView.setBackLabel(backLabel)
        performFeedback()
    }

    private fun showKeyboard() {
        if (!::emojiKeyboardView.isInitialized || !::keyboardView.isInitialized) return
        if (!isEmojiMode) return
        isEmojiMode = false
        emojiKeyboardView.visibility = View.GONE
        keyboardView.visibility = View.VISIBLE
        // 이모지 패널 이전 레이아웃으로 복귀
        currentLayout = prevLayoutBeforeEmoji
        val newEngine = when {
            prevLayoutBeforeEmoji == QwertyLayout || prevLayoutBeforeEmoji is QwertySymbolLayout -> qwertyEngine
            prevLayoutBeforeEmoji is SymbolLayout || prevLayoutBeforeEmoji == NumberLayout -> null
            else -> naratgulEngine
        }
        keyboardView.switchLayout(prevLayoutBeforeEmoji, newEngine)
        keyboardView.inputConnection = currentInputConnection
    }

    private fun performFeedback() {
        if (PreferenceManager.isHapticEnabled(this)) hapticManager.performKeyClick()
        if (PreferenceManager.isSoundEnabled(this)) soundManager.playKeyClick()
    }

    private fun onComposingChanged(composing: String) {
        currentComposingWord = composing
        updateCandidates(composing)
        updateNextKeyHints(composing)
    }

    private fun onWordCommitted(word: String) {
        predictionEngine.onWordConfirmed(word)
        currentComposingWord = ""
        updateCandidates("")
        // 단어 확정 후 힌트 초기화
        if (::keyboardView.isInitialized) keyboardView.applyNextKeyHints(emptyMap())
    }

    private fun onCandidateSelected(word: String) {
        val ic = currentInputConnection ?: return
        ic.finishComposingText()
        if (currentComposingWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentComposingWord.length, 0)
        }
        ic.commitText(word, 1)
        naratgulEngine.reset()
        qwertyEngine.reset()
        predictionEngine.onWordConfirmed(word)
        currentComposingWord = ""
        updateCandidates("")
        performFeedback()
    }

    private fun updateCandidates(prefix: String) {
        if (!::candidateBarView.isInitialized) return
        if (!PreferenceManager.isWordPredictionEnabled(this)) {
            candidateBarView.updateCandidates(emptyList())
            return
        }
        val suggestions = predictionEngine.getSuggestions(prefix)
        candidateBarView.updateCandidates(suggestions)
    }

    /**
     * 현재 입력 중인 단어를 바탕으로 다음에 눌릴 키를 예측하여
     * KeyboardView의 히트 타겟을 미리 확장.
     */
    private fun updateNextKeyHints(composing: String) {
        if (!::keyboardView.isInitialized || !predictionEngine.isReady()) return
        if (!PreferenceManager.isWordPredictionEnabled(this)) {
            keyboardView.applyNextKeyHints(emptyMap())
            return
        }

        val hints: Map<String, Float> = when {
            currentLayout == NaratgulLayout -> {
                // 나랏글 모드: 조합 중인 자소 상태 포함
                val composingText = naratgulEngine.composer.getComposingText()
                val composerState = NaratgulNextKeyPredictor.ComposerState.fromComposingText(composingText)
                // committedLength: currentWordBuffer에서 composingText를 제외한 확정 음절 수
                val committedPart = if (composingText.isNotEmpty() && composing.endsWith(composingText)) {
                    composing.dropLast(composingText.length)
                } else {
                    composing
                }
                predictionEngine.getNextKeyHintsForNaratgul(composing, committedPart.length, composerState)
            }
            currentLayout == QwertyLayout -> {
                predictionEngine.getNextKeyHintsForQwerty(composing)
            }
            else -> emptyMap()
        }

        keyboardView.applyNextKeyHints(hints)
    }

    private fun dpToPx(dp: Float): Float =
        dp * resources.displayMetrics.density
}
