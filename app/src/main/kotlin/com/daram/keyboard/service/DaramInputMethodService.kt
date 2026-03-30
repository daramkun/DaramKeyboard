package com.daram.keyboard.service

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.daram.keyboard.feedback.HapticFeedbackManager
import com.daram.keyboard.feedback.SoundFeedbackManager
import com.daram.keyboard.input.HangulInputEngine
import com.daram.keyboard.input.KeyboardFactory
import com.daram.keyboard.input.KoreanDubeolsikHardwareKeyMapper
import com.daram.keyboard.input.QwertyHardwareKeyMapper
import com.daram.keyboard.input.QwertyInputEngine
import com.daram.keyboard.layout.EmojiLayout
import com.daram.keyboard.layout.KeyboardLayout
import com.daram.keyboard.layout.NaratgulLayout
import com.daram.keyboard.layout.NumberLayout
import com.daram.keyboard.layout.QwertyLayout
import com.daram.keyboard.layout.SymbolLayout
import com.daram.keyboard.model.KoreanKeyboardType
import com.daram.keyboard.prediction.WordPredictionEngine
import com.daram.keyboard.settings.PreferenceManager
import com.daram.nutcracker.SyllableState
import com.daram.nutcracker.prediction.InputLanguage
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

    private val qwertyEngine = QwertyInputEngine()

    /** 현재 활성 한글 입력 엔진 (자판 타입에 따라 교체됨) */
    private var koreanEngine: HangulInputEngine = KeyboardFactory.createKorean(KoreanKeyboardType.NARATGEUL).second
    /** 현재 활성 한글 레이아웃 */
    private var koreanLayout: KeyboardLayout = NaratgulLayout

    private var currentLayout: KeyboardLayout = koreanLayout
    private var currentComposingWord = ""
    private var isEmojiMode = false
    private var prevLayoutBeforeEmoji: KeyboardLayout = koreanLayout

    // 자동 조합 완성 타이머
    private val autoCompositionHandler = Handler(Looper.getMainLooper())
    private var autoCompositionRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        hapticManager = HapticFeedbackManager(this)
        soundManager = SoundFeedbackManager(this).also { it.init() }
        predictionEngine = WordPredictionEngine(this)
        predictionEngine.init()
        applyKoreanKeyboardType(PreferenceManager.getKoreanKeyboardType(this))
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onEvaluateInputViewShown(): Boolean {
        if (!super.onEvaluateInputViewShown()) return false
        // 하드웨어 키보드가 연결되어 있고 설정이 켜져 있으면 소프트 키보드 숨김
        if (PreferenceManager.isHideKeyboardOnHardware(this) && isHardwareKeyboardConnected()) return false
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 언어 전환 키: 다음 IME로 전환
        if (keyCode == KeyEvent.KEYCODE_LANGUAGE_SWITCH) {
            switchToNextInputMethod(false)
            return true
        }
        if (!::keyboardView.isInitialized) return super.onKeyDown(keyCode, event)

        val mapper = if (currentLayout == QwertyLayout) QwertyHardwareKeyMapper
                     else KoreanDubeolsikHardwareKeyMapper
        val shift = if (currentLayout == QwertyLayout) qwertyEngine.getShiftState()
                    else koreanEngine.shiftState
        val action = mapper.mapKeyCode(keyCode, event, shift)
            ?: return super.onKeyDown(keyCode, event)

        keyboardView.handleHardwareKeyAction(action)
        return true
    }

    private fun isHardwareKeyboardConnected(): Boolean {
        val config = resources.configuration
        return config.keyboard != Configuration.KEYBOARD_NOKEYS &&
               config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
    }

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
            inputEngine = koreanEngine,
            onLayoutSwitch = { newLayout -> switchLayout(newLayout) },
            onKeyPressed = { performFeedback() },
            onComposingChanged = { committed, state, composing -> onComposingChanged(committed, state, composing) },
            onWordCommitted = { word -> onWordCommitted(word) },
            onSwitchKoreanType = { switchKoreanType() },
            onSwitchToNextIme = { switchToNextInputMethod(false) }
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
        koreanEngine.reset()
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
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val useOrientationLayout = PreferenceManager.isOrientationLayoutEnabled(this)

        val targetKoreanLayout: KeyboardLayout
        val targetKoreanEngine: HangulInputEngine
        if (isLandscape && useOrientationLayout && !isEnglish) {
            val (layout, engine) = KeyboardFactory.createKorean(KoreanKeyboardType.DUBEOLSIK)
            targetKoreanLayout = layout
            targetKoreanEngine = engine
        } else {
            targetKoreanLayout = koreanLayout
            targetKoreanEngine = koreanEngine
        }

        val targetLayout = if (isEnglish) QwertyLayout else targetKoreanLayout
        if (currentLayout == targetLayout) return
        if (isEmojiMode) {
            prevLayoutBeforeEmoji = targetLayout
            return
        }
        currentLayout = targetLayout
        val newEngine = if (isEnglish) qwertyEngine else targetKoreanEngine
        keyboardView.switchLayout(targetLayout, newEngine)
        keyboardView.inputConnection = currentInputConnection
        koreanEngine.reset()
        qwertyEngine.reset()
    }

    private fun applyKoreanKeyboardType(type: KoreanKeyboardType) {
        val (layout, engine) = KeyboardFactory.createKorean(type)
        koreanLayout = layout
        koreanEngine = engine
    }

    private fun switchKoreanType() {
        val next = PreferenceManager.cycleKoreanKeyboardType(this)
        applyKoreanKeyboardType(next)
        // 현재 한글 레이아웃이 활성화된 상태라면 즉시 교체
        if (!isEmojiMode && currentLayout.isKorean()) {
            currentLayout = koreanLayout
            keyboardView.switchLayout(koreanLayout, koreanEngine)
            keyboardView.inputConnection = currentInputConnection
        }
        if (isEmojiMode && prevLayoutBeforeEmoji.isKorean()) {
            prevLayoutBeforeEmoji = koreanLayout
        }
        performFeedback()
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
        koreanEngine.reset()
        qwertyEngine.reset()
        currentComposingWord = ""
    }

    override fun onDestroy() {
        autoCompositionRunnable?.let { autoCompositionHandler.removeCallbacks(it) }
        soundManager.release()
        super.onDestroy()
    }

    private fun switchLayout(newLayout: KeyboardLayout) {
        if (newLayout is EmojiLayout) {
            showEmojiPanel()
            return
        }

        showKeyboard()
        currentLayout = newLayout
        keyboardView.switchLayout(newLayout, engineForLayout(newLayout))
        keyboardView.inputConnection = currentInputConnection
        performFeedback()
    }

    private fun showEmojiPanel() {
        if (!::emojiKeyboardView.isInitialized || !::keyboardView.isInitialized) return
        prevLayoutBeforeEmoji = currentLayout
        isEmojiMode = true
        val kbHeight = keyboardView.measuredHeight
        if (kbHeight > 0) {
            (emojiKeyboardView.layoutParams as? FrameLayout.LayoutParams)?.height = kbHeight
            emojiKeyboardView.requestLayout()
        }
        keyboardView.visibility = View.GONE
        emojiKeyboardView.visibility = View.VISIBLE
        emojiKeyboardView.selectCategory(0)
        emojiKeyboardView.setSkinTone(PreferenceManager.getEmojiSkinTone(this))
        val backLabel = currentLayout.returnButtonLabel
        emojiKeyboardView.setBackLabel(backLabel)
        performFeedback()
    }

    private fun showKeyboard() {
        if (!::emojiKeyboardView.isInitialized || !::keyboardView.isInitialized) return
        if (!isEmojiMode) return
        isEmojiMode = false
        emojiKeyboardView.visibility = View.GONE
        keyboardView.visibility = View.VISIBLE
        currentLayout = prevLayoutBeforeEmoji
        keyboardView.switchLayout(prevLayoutBeforeEmoji, engineForLayout(prevLayoutBeforeEmoji))
        keyboardView.inputConnection = currentInputConnection
    }

    private fun engineForLayout(layout: KeyboardLayout) = when {
        layout == QwertyLayout -> qwertyEngine
        layout is SymbolLayout || layout == NumberLayout -> null
        else -> koreanEngine
    }

    private fun KeyboardLayout.isKorean(): Boolean =
        this != QwertyLayout && this !is SymbolLayout && this != NumberLayout

    private fun performFeedback() {
        val intensity = PreferenceManager.getHapticIntensity(this)
        if (intensity != "off") hapticManager.performKeyClick(intensity)
        if (PreferenceManager.isSoundEnabled(this)) soundManager.playKeyClick()
    }

    private fun onComposingChanged(committed: String, state: SyllableState, composing: String) {
        currentComposingWord = committed + composing
        updateCandidates(committed, state, composing)
        updateNextKeyHints(committed, state, composing)
        rescheduleAutoComposition(composing.isNotEmpty())
    }

    private fun rescheduleAutoComposition(hasComposing: Boolean) {
        autoCompositionRunnable?.let { autoCompositionHandler.removeCallbacks(it) }
        autoCompositionRunnable = null
        if (!hasComposing) return
        val timeoutMs = PreferenceManager.getAutoCompositionTimeout(this)
        if (timeoutMs <= 0) return
        autoCompositionRunnable = Runnable {
            val ic = currentInputConnection ?: return@Runnable
            ic.finishComposingText()
            koreanEngine.reset()
            val word = currentComposingWord.trim()
            currentComposingWord = ""
            if (word.isNotEmpty()) {
                val language = com.daram.nutcracker.prediction.InputLanguage.KOREAN
                predictionEngine.onWordCommitted(word, language)
            }
            updateCandidates("", SyllableState(), "")
            if (::keyboardView.isInitialized) keyboardView.applyNextKeyHints(emptyMap())
        }
        autoCompositionHandler.postDelayed(autoCompositionRunnable!!, timeoutMs.toLong())
    }

    private fun onWordCommitted(word: String) {
        val language = if (currentLayout == QwertyLayout) InputLanguage.ENGLISH else InputLanguage.KOREAN
        predictionEngine.onWordCommitted(word, language)
        currentComposingWord = ""
        updateCandidates("", SyllableState(), "")
        if (::keyboardView.isInitialized) keyboardView.applyNextKeyHints(emptyMap())
    }

    private fun onCandidateSelected(word: String) {
        val ic = currentInputConnection ?: return
        ic.finishComposingText()
        if (currentComposingWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentComposingWord.length, 0)
        }
        ic.commitText(word, 1)
        koreanEngine.reset()
        qwertyEngine.reset()
        val language = if (currentLayout == QwertyLayout) InputLanguage.ENGLISH else InputLanguage.KOREAN
        predictionEngine.onCandidateSelected(word, language)
        currentComposingWord = ""
        updateCandidates("", SyllableState(), "")
        performFeedback()
    }

    private fun updateCandidates(committed: String, state: SyllableState, composing: String) {
        if (!::candidateBarView.isInitialized) return
        if (!PreferenceManager.isWordPredictionEnabled(this)) {
            candidateBarView.updateCandidates(emptyList())
            return
        }
        val language = if (currentLayout == QwertyLayout) InputLanguage.ENGLISH else InputLanguage.KOREAN
        val suggestions = predictionEngine.getSuggestions(committed, state, composing, language)
        candidateBarView.updateCandidates(suggestions)
    }

    private fun updateNextKeyHints(committed: String, state: SyllableState, composing: String) {
        if (!::keyboardView.isInitialized || !predictionEngine.isReady()) return
        if (!PreferenceManager.isWordPredictionEnabled(this)) {
            keyboardView.applyNextKeyHints(emptyMap())
            return
        }

        val hints: Map<String, Float> = when {
            currentLayout.isKorean() -> {
                val type = PreferenceManager.getKoreanKeyboardType(this)
                predictionEngine.getNextKeyHintsForHangul(
                    committedText = committed,
                    syllableState = state,
                    composingText = composing,
                    keyMapper = type.keyMapper(),
                    ambiguityResolver = type.ambiguityResolver()
                )
            }
            currentLayout == QwertyLayout ->
                predictionEngine.getNextKeyHintsForQwerty(committed + composing)
            else -> emptyMap()
        }

        keyboardView.applyNextKeyHints(hints)
    }

    private fun dpToPx(dp: Float): Float =
        dp * resources.displayMetrics.density
}
