package com.daram.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.daram.keyboard.theme.KeyboardTheme

class CandidateBarView(
    context: Context,
    private var theme: KeyboardTheme,
    private val onCandidateSelected: (String) -> Unit
) : View(context) {

    private var candidates: List<String> = emptyList()

    private val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = spToPx(15f)
    }
    private val dividerPaint = Paint().apply { style = Paint.Style.FILL }
    private val pressedPaint = Paint().apply { style = Paint.Style.FILL }

    private val candidateRects = mutableListOf<RectF>()
    private var pressedIndex = -1

    private fun spToPx(sp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    private fun dpToPx(dp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    init {
        applyTheme(theme)
    }

    fun applyTheme(newTheme: KeyboardTheme) {
        theme = newTheme
        backgroundPaint.color = theme.keyFunctionBackground
        labelPaint.color = theme.keyLabelColor
        dividerPaint.color = theme.dividerColor
        pressedPaint.color = theme.keyPressedBackground
        invalidate()
    }

    fun updateCandidates(newCandidates: List<String>) {
        candidates = newCandidates
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        recalculateRects(w, h)
    }

    private fun recalculateRects(w: Int, h: Int) {
        candidateRects.clear()
        if (candidates.isEmpty()) return
        val cellWidth = w.toFloat() / candidates.size
        for (i in candidates.indices) {
            candidateRects.add(RectF(i * cellWidth, 0f, (i + 1) * cellWidth, h.toFloat()))
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (candidates.isEmpty()) return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        recalculateRects(width, height)

        for (i in candidates.indices) {
            val rect = candidateRects.getOrNull(i) ?: continue

            // 눌린 상태
            if (i == pressedIndex) {
                canvas.drawRect(rect, pressedPaint)
            }

            // 구분선 (마지막 제외)
            if (i < candidates.size - 1) {
                val dividerW = dpToPx(1f)
                canvas.drawRect(rect.right - dividerW / 2, rect.top + dpToPx(8f),
                    rect.right + dividerW / 2, rect.bottom - dpToPx(8f), dividerPaint)
            }

            // 텍스트
            val textY = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(candidates[i], rect.centerX(), textY, labelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedIndex = findCandidateIndex(event.x)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val idx = findCandidateIndex(event.x)
                if (idx >= 0 && idx == pressedIndex && idx < candidates.size) {
                    onCandidateSelected(candidates[idx])
                }
                pressedIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                invalidate()
            }
        }
        return true
    }

    private fun findCandidateIndex(x: Float): Int {
        return candidateRects.indexOfFirst { it.contains(x, height / 2f) }
    }
}
