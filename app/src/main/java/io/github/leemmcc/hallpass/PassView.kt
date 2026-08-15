package io.github.leemmcc.hallpass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class PassView(context: Context) : View(context) {

    private var state: PassState = PassState.GREEN
    private var elapsedText: String = "0:00"

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TIMER_TEXT
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val path = Path()

    fun render(state: PassState, elapsedText: String) {
        if (state == this.state && elapsedText == this.elapsedText) return
        this.state = state
        this.elapsedText = elapsedText
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        val short = minOf(w, h)
        val glyph = short * GLYPH_FRACTION
        val cx = w / 2f
        val cy = h / 2f

        when (state) {
            PassState.GREEN -> {
                canvas.drawColor(GREEN)
                strokePaint.strokeWidth = glyph * STROKE_FRACTION
                path.reset()
                path.moveTo(cx - glyph / 2f, cy)
                path.lineTo(cx - glyph / 8f, cy + glyph / 3f)
                path.lineTo(cx + glyph / 2f, cy - glyph / 3f)
                canvas.drawPath(path, strokePaint)
            }

            PassState.YELLOW -> {
                canvas.drawColor(YELLOW)
                textPaint.textSize = glyph
                // Centre the text on its own vertical midpoint, not the baseline.
                val metrics = textPaint.fontMetrics
                val baseline = cy - (metrics.ascent + metrics.descent) / 2f
                canvas.drawText(elapsedText, cx, baseline, textPaint)
            }

            PassState.RED -> {
                canvas.drawColor(RED)
                strokePaint.strokeWidth = glyph * STROKE_FRACTION
                val r = glyph / 2f
                canvas.drawLine(cx - r, cy - r, cx + r, cy + r, strokePaint)
                canvas.drawLine(cx + r, cy - r, cx - r, cy + r, strokePaint)
            }
        }
    }

    private companion object {
        const val GREEN = 0xFF2E7D32.toInt()
        const val YELLOW = 0xFFF9A825.toInt()
        const val RED = 0xFFC62828.toInt()
        const val TIMER_TEXT = 0xFF1A1A1A.toInt()

        /** Glyph size as a fraction of the shorter screen dimension. */
        const val GLYPH_FRACTION = 0.40f
        const val STROKE_FRACTION = 0.14f
    }
}
