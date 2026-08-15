package io.github.leemmcc.hallpass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
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

    /** Reused by every measurement below; onDraw allocates nothing. */
    private val textBounds = Rect()

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
                fitTextSize(elapsedText, targetHeight = glyph, maxWidth = w * MAX_TEXT_WIDTH_FRACTION)
                // Centre the text on its own vertical midpoint, not the baseline.
                textPaint.getTextBounds(elapsedText, 0, elapsedText.length, textBounds)
                val baseline = cy - (textBounds.top + textBounds.bottom) / 2f
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

    /**
     * Leaves textPaint sized so the digits *render* [targetHeight] tall and no
     * wider than [maxWidth].
     *
     * textSize is the em size, not the rendered glyph height: Roboto digits
     * occupy roughly 0.71em, so setting textSize = targetHeight directly draws
     * digits about 30% shorter than the check and the cross, against a spec
     * that asks all three states to carry the same weight from the back of the
     * room. So measure and correct.
     *
     * The width clamp is what keeps "72:15" -- the reading that matters -- on
     * screen on a narrow portrait tablet, where digits at the full symbol
     * height would run past both edges.
     *
     * Text metrics scale linearly with textSize, so one correction pass each
     * way lands within a hinting rounding error.
     */
    private fun fitTextSize(text: String, targetHeight: Float, maxWidth: Float) {
        if (text.isEmpty() || targetHeight <= 0f) return

        textPaint.textSize = targetHeight
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val renderedHeight = textBounds.height().toFloat()
        var size = targetHeight
        if (renderedHeight > 0f) size = targetHeight * targetHeight / renderedHeight

        textPaint.textSize = size
        val renderedWidth = textPaint.measureText(text)
        if (maxWidth > 0f && renderedWidth > maxWidth) {
            size *= maxWidth / renderedWidth
            textPaint.textSize = size
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

        /** Widest the timer may render, as a fraction of the screen width. */
        const val MAX_TEXT_WIDTH_FRACTION = 0.90f
    }
}
