package com.daram.keyboard.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.inputmethod.InputConnection
import com.daram.keyboard.hittarget.HitTargetManager
import com.daram.keyboard.settings.PreferenceManager
import com.daram.keyboard.input.HangulInputEngine
import com.daram.keyboard.input.InputEngine
import com.daram.keyboard.input.QwertyInputEngine
import com.daram.keyboard.input.ShiftState
import com.daram.keyboard.layout.EmojiLayout
import com.daram.keyboard.layout.KeyboardLayout
import com.daram.keyboard.layout.NaratgulLayout
import com.daram.keyboard.layout.NumberLayout
import com.daram.keyboard.layout.QwertyLayout
import com.daram.keyboard.layout.SecondaryLabelAlignment
import com.daram.keyboard.layout.SymbolLayout
import com.daram.keyboard.model.Key
import com.daram.keyboard.model.KeyAction
import com.daram.keyboard.model.KeyStyle
import com.daram.keyboard.theme.KeyboardTheme
import com.daram.nutcracker.SyllableState

class KeyboardView(
    context: Context,
    private var theme: KeyboardTheme,
    private var inputEngine: InputEngine,
    private val onLayoutSwitch: (KeyboardLayout) -> Unit,
    private val onKeyPressed: () -> Unit = {},
    /** 현재 입력 중인 단어 예측용 콜백: 확정 부분, FSM 상태, 조합 중인 음절 */
    private val onComposingChanged: (committedPart: String, syllableState: SyllableState, composingText: String) -> Unit = { _, _, _ -> },
    /** 스페이스/엔터로 단어가 완전히 확정될 때 */
    private val onWordCommitted: (String) -> Unit = {},
    /** 한국어 자판 종류 전환 요청 */
    private val onSwitchKoreanType: () -> Unit = {},
    /** 시스템 다음 IME로 전환 요청 */
    private val onSwitchToNextIme: () -> Unit = {}
) : View(context) {

    private var layout: KeyboardLayout = NaratgulLayout
    private val hitTargetManager = HitTargetManager()

    var isLandscapeMode: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (width > 0 && height > 0) calculateKeyRects(width, height - gestureNavBottomPadding)
            invalidate()
        }

    var inputConnection: InputConnection? = null

    // Paint (1회 생성)
    private val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    private val keyBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val keyActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val secondaryLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
    }

    private val keyRects = mutableMapOf<String, RectF>()

    // 히트 타겟 시각화
    private val hitTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(200, 255, 0, 0)  // 빨간색: 히트 영역 경계
    }
    private val visualRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(160, 0, 120, 255)  // 파란색: 시각 영역 경계 (비교용)
    }

    // 터치 상태
    private var pressedKey: Key? = null
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressTriggered = false
    private val longPressDelayMs = 400L

    // 백스페이스 반복 삭제
    private var backspaceRepeatRunnable: Runnable? = null
    private val backspaceRepeatIntervalMs = 50L

    // 이모지 스와이프 감지
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var isSwipeCandidate = false
    private val swipeThresholdPx by lazy { dpToPx(40f) }

    // 더블 스페이스 자동 온점
    private var lastSpaceTimeMs = 0L
    private val doubleSpaceThresholdMs = 500L

    // 키 팝업 페인트
    private val popupBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val popupLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // 현재 입력 중인 단어 버퍼
    private val currentWordBuffer = StringBuilder()

    // 이전 레이아웃 추적 (기호/숫자/이모지 복귀용)
    private var prevLayout: KeyboardLayout = NaratgulLayout

    private fun Key.isHiddenInLandscape() =
        isLandscapeMode && (action is KeyAction.SwitchKoreanType || longPressAction is KeyAction.SwitchKoreanType)

    private fun dpToPx(dp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun spToPx(sp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    private val dividerWidthPx = dpToPx(1f)
    private val secondaryLabelPaddingPx = dpToPx(3f)

    init {
        applyTheme(theme)
        attachEngineCallbacks()
    }

    /** HangulInputEngine에 commit 추적 콜백 연결 */
    private fun attachEngineCallbacks() {
        (inputEngine as? HangulInputEngine)?.onTextCommitted = { committed ->
            currentWordBuffer.append(committed)
        }
    }

    fun applyTheme(newTheme: KeyboardTheme) {
        theme = newTheme
        backgroundPaint.color = theme.backgroundColor
        keyActivePaint.color = theme.keyPressedBackground
        labelPaint.textSize = spToPx(theme.keyLabelSizeSp)
        labelPaint.color = theme.keyLabelColor
        secondaryLabelPaint.textSize = spToPx(theme.keySecondaryLabelSizeSp)
        secondaryLabelPaint.color = theme.keySecondaryLabelColor
        popupBgPaint.color = theme.keyPressedBackground
        popupLabelPaint.color = theme.keyLabelColor
        popupLabelPaint.textSize = spToPx(theme.keyLabelSizeSp)
        invalidate()
    }

    fun switchLayout(newLayout: KeyboardLayout, newEngine: InputEngine? = null) {
        layout = newLayout
        if (newEngine != null) {
            inputEngine = newEngine
            attachEngineCallbacks()
        }
        currentWordBuffer.clear()
        if (width > 0 && height > 0) {
            calculateKeyRects(width, height - gestureNavBottomPadding)
        }
        invalidate()
    }

    // 제스처 네비게이션 시 추가할 하단 여백 (px)
    private var gestureNavBottomPadding = 0

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        readNavInsets()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        readNavInsets()
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        super.onDetachedFromWindow()
    }

    private fun readNavInsets() {
        val insets = rootWindowInsets ?: return
        val navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        val tappableBottom = insets.getInsets(WindowInsets.Type.tappableElement()).bottom
        val systemBottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom
        val bottomPx = maxOf(navBottom, tappableBottom, systemBottom)
        if (bottomPx != gestureNavBottomPadding) {
            gestureNavBottomPadding = bottomPx
            requestLayout()
        }
    }

    fun updateGestureNavPadding(bottomPx: Int) {
        if (bottomPx != gestureNavBottomPadding) {
            gestureNavBottomPadding = bottomPx
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val keyboardHeight = when (PreferenceManager.getKeyboardHeight(context)) {
            "small" -> dpToPx(if (isLandscape) 160f else 240f).toInt()
            "large" -> dpToPx(if (isLandscape) 220f else 320f).toInt()
            else    -> dpToPx(if (isLandscape) 190f else 280f).toInt()
        }
        setMeasuredDimension(width, keyboardHeight + gestureNavBottomPadding)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 키 영역은 gestureNavBottomPadding을 제외한 높이로 계산
        calculateKeyRects(w, h - gestureNavBottomPadding)
    }

    private fun calculateKeyRects(totalWidth: Int, totalHeight: Int) {
        keyRects.clear()
        val rows = layout.rows
        val rowHeight = totalHeight.toFloat() / rows.size
        val startXRatios = layout.rowStartXRatios

        for (rowIdx in rows.indices) {
            val row = rows[rowIdx]
            val totalWeight = row.sumOf { it.widthWeight.toDouble() }.toFloat()
            val startX = startXRatios.getOrElse(rowIdx) { 0f } * totalWidth
            val pixelsPerWeight = (totalWidth - startX) / totalWeight
            var x = startX
            for (key in row) {
                val keyWidth = key.widthWeight * pixelsPerWeight
                if (key.id !in keyRects) {
                    keyRects[key.id] = RectF(
                        x, rowIdx * rowHeight,
                        x + keyWidth, rowIdx * rowHeight + rowHeight * key.rowSpan
                    )
                }
                x += keyWidth
            }
        }

        val allKeys = layout.rows.flatten().distinctBy { it.id }
            .filter { it.visible && !it.isHiddenInLandscape() }
        hitTargetManager.setKeyRects(allKeys, keyRects)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        for (row in layout.rows) {
            for (key in row) {
                if (!key.visible || key.isHiddenInLandscape()) continue
                keyRects[key.id]?.let { drawKey(canvas, key, it) }
            }
        }
        // 키 팝업 오버레이
        val popupKey = pressedKey
        if (popupKey != null && PreferenceManager.isKeyPopupEnabled(context)) {
            keyRects[popupKey.id]?.let { keyRect ->
                val popupW = keyRect.width() * 1.1f
                val popupH = keyRect.height() * 1.1f
                val popupL = (keyRect.centerX() - popupW / 2f).coerceAtLeast(0f)
                val popupR = (popupL + popupW).coerceAtMost(width.toFloat())
                val aboveB = keyRect.top - dpToPx(4f)
                val aboveT = (aboveB - popupH).coerceAtLeast(0f)
                val popupT = aboveT
                val popupB = popupT + popupH
                val radius = dpToPx(6f)
                canvas.drawRoundRect(popupL, popupT, popupR, popupB, radius, radius, popupBgPaint)
                val label = when (popupKey.style) {
                    KeyStyle.BACKSPACE -> "\u232B"
                    KeyStyle.ENTER     -> "\u21B5"
                    KeyStyle.SHIFT     -> "\u2191"
                    KeyStyle.SPACE     -> null
                    KeyStyle.RETURN    -> returnLabel()
                    else               -> popupKey.primaryLabel.takeIf { it.isNotEmpty() }
                }
                if (label != null) {
                    val textY = (popupT + popupB) / 2f - (popupLabelPaint.descent() + popupLabelPaint.ascent()) / 2f
                    canvas.drawText(label, keyRect.centerX(), textY, popupLabelPaint)
                }
            }
        }

        // 히트 타겟 시각화 오버레이
        if (PreferenceManager.isShowHitTargetsEnabled(context)) {
            canvas.save()
            canvas.clipRect(0f, 0f, width.toFloat(), (height - gestureNavBottomPadding).toFloat())
            for (row in layout.rows) {
                for (key in row) {
                    if (!key.visible) continue
                    // 파란 테두리: 시각적 키 영역
                    keyRects[key.id]?.let { visualRect ->
                        canvas.drawRect(visualRect, visualRectPaint)
                    }
                    // 빨간 테두리: 실제 히트 영역 (Voronoi 클리핑 적용)
                    hitTargetManager.getHitRect(key.id)?.let { hitRect ->
                        canvas.drawRect(hitRect, hitTargetPaint)
                    }
                }
            }
            canvas.restore()
        }
    }

    private fun isCurrentEmojiCategory(key: Key): Boolean {
        val emojiLayout = layout as? EmojiLayout ?: return false
        return key.action is KeyAction.SwitchEmojiCategory &&
                (key.action as KeyAction.SwitchEmojiCategory).categoryIndex == emojiLayout.categoryIndex
    }

    /** ReturnToPrev 버튼에 표시할 레이블: 이전 레이아웃의 메타데이터에서 결정 */
    private fun returnLabel(): String = prevLayout.returnButtonLabel

    private fun drawKey(canvas: Canvas, key: Key, rect: RectF) {
        val isPressed = pressedKey?.id == key.id
        val isActiveCategory = isCurrentEmojiCategory(key)

        keyBackgroundPaint.color = when {
            isPressed -> theme.keyPressedBackground
            isActiveCategory -> theme.keyNormalBackground  // 활성 카테고리는 밝게
            key.style == KeyStyle.NORMAL || key.style == KeyStyle.SPACE -> theme.keyNormalBackground
            else -> theme.keyFunctionBackground
        }
        canvas.drawRect(
            rect.left + dividerWidthPx, rect.top + dividerWidthPx,
            rect.right - dividerWidthPx, rect.bottom - dividerWidthPx,
            keyBackgroundPaint
        )

        val cx = rect.centerX()
        val textY = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
        labelPaint.color = theme.keyLabelColor

        when (key.style) {
            KeyStyle.BACKSPACE -> canvas.drawText("\u232B", cx, textY, labelPaint)
            KeyStyle.ENTER -> {
                // 엔터 아이콘은 1.4배 크게
                val savedSize = labelPaint.textSize
                labelPaint.textSize = savedSize * 1.4f
                val bigTextY = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
                canvas.drawText("\u21B5", cx, bigTextY, labelPaint)
                labelPaint.textSize = savedSize
            }
            KeyStyle.SPACE -> {
                // 스페이스 아이콘: 밑줄 바 형태
                val barY = rect.centerY() + spToPx(theme.keyLabelSizeSp) * 0.1f
                val barHalfWidth = rect.width() * 0.28f
                val barHeight = dpToPx(2f)
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = labelPaint.color
                    alpha = 160
                }
                canvas.drawRoundRect(
                    cx - barHalfWidth, barY - barHeight / 2f,
                    cx + barHalfWidth, barY + barHeight / 2f,
                    barHeight, barHeight, barPaint
                )
            }
            KeyStyle.RETURN    -> canvas.drawText(returnLabel(), cx, textY, labelPaint)
            KeyStyle.SHIFT     -> {
                val shiftState = (inputEngine as? QwertyInputEngine)?.getShiftState()
                    ?: (inputEngine as? HangulInputEngine)?.takeIf { it.hasShift }?.shiftState
                    ?: ShiftState.OFF
                val icon = when (shiftState) {
                    ShiftState.OFF    -> "\u2191"  // ↑
                    ShiftState.ONCE   -> "\u21E7"  // ⇧
                    ShiftState.LOCKED -> "\u21EA"  // ⇪
                }
                if (shiftState == ShiftState.LOCKED) {
                    labelPaint.color = theme.keyPressedBackground
                } else if (shiftState == ShiftState.ONCE) {
                    labelPaint.color = theme.keyLabelColor
                }
                canvas.drawText(icon, cx, textY, labelPaint)
                labelPaint.color = theme.keyLabelColor
            }
            else -> if (key.primaryLabel.isNotEmpty()) {
                canvas.drawText(key.primaryLabel, cx, textY, labelPaint)
            }
        }

        if (key.secondaryLabel != null) {
            val showSecondaryOnRight = layout.secondaryLabelAlignment == SecondaryLabelAlignment.BOTTOM_RIGHT
            if (showSecondaryOnRight) {
                secondaryLabelPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(key.secondaryLabel,
                    rect.right - secondaryLabelPaddingPx,
                    rect.bottom - secondaryLabelPaddingPx,
                    secondaryLabelPaint)
            } else {
                secondaryLabelPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(key.secondaryLabel,
                    rect.left + secondaryLabelPaddingPx,
                    rect.top + secondaryLabelPaddingPx + spToPx(theme.keySecondaryLabelSizeSp),
                    secondaryLabelPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)
                    event.getX(event.actionIndex) else event.x
                val y = if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)
                    event.getY(event.actionIndex) else event.y

                // 이모지 레이아웃에서 스와이프 감지 초기화
                if (layout is EmojiLayout) {
                    swipeStartX = x
                    swipeStartY = y
                    isSwipeCandidate = true
                } else {
                    isSwipeCandidate = false
                }

                val key = hitTargetManager.findKeyAt(x, y) ?: return true
                pressedKey = key
                isLongPressTriggered = false

                if (key.action is KeyAction.Backspace) {
                    // 백스페이스: 롱프레스 후 반복 삭제
                    longPressRunnable = Runnable {
                        isLongPressTriggered = true
                        scheduleBackspaceRepeat()
                    }
                    handler.postDelayed(longPressRunnable!!, longPressDelayMs)
                } else if (key.longPressAction != null) {
                    longPressRunnable = Runnable {
                        isLongPressTriggered = true
                        handleKeyAction(key.longPressAction)
                        pressedKey = null
                        invalidate()
                    }
                    handler.postDelayed(longPressRunnable!!, longPressDelayMs)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                longPressRunnable = null
                cancelBackspaceRepeat()

                val x = if (event.actionMasked == MotionEvent.ACTION_POINTER_UP)
                    event.getX(event.actionIndex) else event.x
                val y = if (event.actionMasked == MotionEvent.ACTION_POINTER_UP)
                    event.getY(event.actionIndex) else event.y

                // 이모지 스와이프 처리
                if (isSwipeCandidate && layout is EmojiLayout) {
                    val dx = x - swipeStartX
                    val dy = y - swipeStartY
                    if (Math.abs(dx) > swipeThresholdPx && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                        val emojiLayout = layout as EmojiLayout
                        val nextCategory = if (dx < 0) {
                            (emojiLayout.categoryIndex + 1) % EmojiLayout.CATEGORY_COUNT
                        } else {
                            (emojiLayout.categoryIndex - 1 + EmojiLayout.CATEGORY_COUNT) % EmojiLayout.CATEGORY_COUNT
                        }
                        onLayoutSwitch(EmojiLayout.of(nextCategory))
                        pressedKey = null
                        isSwipeCandidate = false
                        isLongPressTriggered = false
                        invalidate()
                        return true
                    }
                }
                isSwipeCandidate = false

                if (!isLongPressTriggered) {
                    pressedKey?.let { key ->
                        // 한글 키 액션이면 자소를 먼저 처리한 뒤 lastInputJamo를 읽어야 하므로
                        // handleKeyAction 전에 임시로 action을 확인, TypeHangul이면 처리 후 jamo를 획득
                        val hangulEngine = inputEngine as? HangulInputEngine
                        val isHangulKey = key.action is KeyAction.TypeHangul || key.action is KeyAction.TypeRawKey
                        if (isHangulKey && hangulEngine != null) {
                            handleKeyAction(key.action)
                            // 처리 후 실제 반영된 자소를 히트 타겟에 기록
                            hitTargetManager.recordTouch(x, y, key, hangulEngine.lastInputJamo)
                        } else {
                            hitTargetManager.recordTouch(x, y, key)
                            handleKeyAction(key.action)
                        }
                        onKeyPressed()
                    }
                }
                pressedKey = null
                isLongPressTriggered = false
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                longPressRunnable = null
                cancelBackspaceRepeat()
                pressedKey = null
                isLongPressTriggered = false
                isSwipeCandidate = false
                invalidate()
            }
        }
        return true
    }

    private fun scheduleBackspaceRepeat() {
        val ic = inputConnection ?: return
        // 즉시 한 번 삭제
        performBackspace(ic)
        backspaceRepeatRunnable = object : Runnable {
            override fun run() {
                if (pressedKey?.action is KeyAction.Backspace) {
                    performBackspace(ic)
                    handler.postDelayed(this, backspaceRepeatIntervalMs)
                }
            }
        }
        handler.postDelayed(backspaceRepeatRunnable!!, backspaceRepeatIntervalMs)
    }

    private fun cancelBackspaceRepeat() {
        backspaceRepeatRunnable?.let { handler.removeCallbacks(it) }
        backspaceRepeatRunnable = null
    }

    private fun resetComposing(ic: InputConnection, clearWord: Boolean = true) {
        ic.finishComposingText()
        inputEngine.reset()
        if (clearWord) currentWordBuffer.clear()
    }

    private fun performBackspace(ic: InputConnection) {
        val eng = inputEngine as? HangulInputEngine
        val hadComposing = eng?.isEmpty() == false
        inputEngine.processAction(KeyAction.Backspace, ic)
        if (!hadComposing && currentWordBuffer.isNotEmpty()) {
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
        }
        notifyComposingChanged()
        invalidate()
    }

    private fun handleKeyAction(action: KeyAction) {
        val ic = inputConnection ?: return

        when (action) {
            is KeyAction.SwitchToNaratgul -> {
                resetComposing(ic)
                prevLayout = NaratgulLayout
                onLayoutSwitch(NaratgulLayout)
            }
            is KeyAction.SwitchToQwerty -> {
                resetComposing(ic)
                prevLayout = QwertyLayout
                onLayoutSwitch(QwertyLayout)
            }
            is KeyAction.SwitchToNumber -> {
                resetComposing(ic)
                if (!layout.isAuxiliary) prevLayout = layout
                onLayoutSwitch(NumberLayout)
            }
            is KeyAction.SwitchToSymbol -> {
                resetComposing(ic)
                if (!layout.isAuxiliary) prevLayout = layout
                onLayoutSwitch(SymbolLayout.Page1)
            }
            is KeyAction.SwitchToQwertySymbol -> {
                resetComposing(ic)
                if (!layout.isAuxiliary) prevLayout = layout
                onLayoutSwitch(SymbolLayout.QwertyPage1)
            }
            is KeyAction.SymbolNextPage -> {
                val current = layout
                val next = if (current is SymbolLayout) SymbolLayout.nextPage(current)
                           else SymbolLayout.Page1
                onLayoutSwitch(next)
            }
            is KeyAction.ShowEmoji -> {
                resetComposing(ic, clearWord = false)
                if (!layout.isAuxiliary) prevLayout = layout
                onLayoutSwitch(EmojiLayout.of(0))
            }
            is KeyAction.SwitchEmojiCategory -> {
                onLayoutSwitch(EmojiLayout.of(action.categoryIndex))
            }
            is KeyAction.ReturnToPrev -> {
                resetComposing(ic)
                onLayoutSwitch(prevLayout)
            }
            is KeyAction.SwitchKoreanType -> {
                resetComposing(ic)
                onSwitchKoreanType()
            }
            is KeyAction.SwitchToNextIme -> {
                resetComposing(ic)
                onSwitchToNextIme()
            }

            is KeyAction.Space -> {
                val now = System.currentTimeMillis()
                val isDoubleSpace = PreferenceManager.isAutoPeriodOnDoubleSpaceEnabled(context) &&
                        (now - lastSpaceTimeMs) < doubleSpaceThresholdMs &&
                        currentWordBuffer.isNotEmpty()
                lastSpaceTimeMs = now

                if (isDoubleSpace) {
                    // 직전 스페이스를 지우고 ". " 입력
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(". ", 1)
                    val word = currentWordBuffer.toString().trim()
                    currentWordBuffer.clear()
                    if (word.isNotEmpty()) onWordCommitted(word)
                    onComposingChanged("", SyllableState(), "")
                } else {
                    inputEngine.processAction(action, ic)
                    val word = currentWordBuffer.toString().trim()
                    currentWordBuffer.clear()
                    if (word.isNotEmpty()) onWordCommitted(word)
                    onComposingChanged("", SyllableState(), "")
                }
            }
            is KeyAction.Enter -> {
                inputEngine.processAction(action, ic)
                val word = currentWordBuffer.toString().trim()
                currentWordBuffer.clear()
                if (word.isNotEmpty()) onWordCommitted(word)
                onComposingChanged("", SyllableState(), "")
            }
            is KeyAction.TypeNumber -> {
                val word = currentWordBuffer.toString().trim()
                currentWordBuffer.clear()
                if (word.isNotEmpty()) onWordCommitted(word)
                inputEngine.processAction(action, ic)
                onComposingChanged("", SyllableState(), "")
            }
            is KeyAction.Backspace -> {
                // 단일 탭 처리 (롱프레스 반복은 scheduleBackspaceRepeat에서 처리)
                performBackspace(ic)
                return  // invalidate()는 performBackspace에서 호출
            }
            is KeyAction.TypeText -> {
                inputEngine.processAction(action, ic)
                currentWordBuffer.append(action.text)
                notifyComposingChanged()
            }
            is KeyAction.TypeHangul, is KeyAction.AddStroke, is KeyAction.ToggleDouble,
            is KeyAction.TypeRawKey -> {
                inputEngine.processAction(action, ic)
                notifyComposingChanged()
            }
            else -> inputEngine.processAction(action, ic)
        }

        invalidate()
    }

    private fun notifyComposingChanged() {
        val hangulEngine = inputEngine as? HangulInputEngine
        val composingText = hangulEngine?.getComposingText() ?: ""
        val syllableState = hangulEngine?.syllableState ?: SyllableState()
        onComposingChanged(currentWordBuffer.toString(), syllableState, composingText)
    }

    /**
     * 예측 엔진이 계산한 다음 키 힌트를 히트 타겟 매니저에 적용.
     * DaramInputMethodService에서 onComposingChanged 콜백 후 호출.
     */
    fun applyNextKeyHints(hints: Map<String, Float>) {
        hitTargetManager.setNextKeyHints(hints)
        invalidate()
    }

    /**
     * 하드웨어 키보드 입력을 터치 입력과 동일한 경로로 처리한다.
     * DaramInputMethodService.onKeyDown()에서 호출.
     *
     * @param engineOverride null이면 현재 inputEngine 사용.
     *   나랏글·천지인 등 소프트 전용 자판에서 하드웨어 입력 시
     *   DubeolsikAutomata 기반 엔진을 전달해 올바른 조합을 보장한다.
     */
    fun handleHardwareKeyAction(action: KeyAction, engineOverride: InputEngine? = null) {
        if (engineOverride != null) {
            val saved = inputEngine
            inputEngine = engineOverride
            attachEngineCallbacks()
            handleKeyAction(action)
            onKeyPressed()
            inputEngine = saved
            attachEngineCallbacks()
        } else {
            handleKeyAction(action)
            onKeyPressed()
        }
    }
}
