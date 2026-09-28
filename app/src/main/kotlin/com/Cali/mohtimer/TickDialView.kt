package com.Cali.mohtimer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

/**
 * Minimalist dial: one dim background ring + a smooth accent-colored progress arc.
 * No tick marks, no glow. Just a clean circle.
 */
class TickDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var displayedProgress = 0f
    private var accentColor = Color.parseColor("#FF00E5")
    private var progressAnimator: ValueAnimator? = null

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#2AFFFFFF")
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val arcRect = RectF()

    fun setAccentColor(color: Int) {
        accentColor = color
        invalidate()
    }

    /** Smoothly animates to the target progress. */
    fun setProgress(p: Int) {
        val clamped = p.coerceIn(0, 100).toFloat()
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(displayedProgress, clamped).apply {
            duration = 250L
            interpolator = LinearInterpolator()
            addUpdateListener {
                displayedProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Instantly set progress — used when resetting to IDLE. */
    fun setProgressImmediate(p: Int) {
        progressAnimator?.cancel()
        displayedProgress = p.coerceIn(0, 100).toFloat()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val strokeWidth = (min(cx, cy) * 0.08f).coerceAtLeast(8f)
        val radius = min(cx, cy) - strokeWidth

        ringPaint.strokeWidth = strokeWidth
        arcPaint.strokeWidth = strokeWidth
        arcPaint.color = accentColor

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Background ring
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // Progress arc
        val sweepAngle = 360f * (displayedProgress / 100f)
        if (sweepAngle > 0.5f) {
            canvas.drawArc(arcRect, -90f, sweepAngle, false, arcPaint)
        }
    }

    override fun onDetachedFromWindow() {
        progressAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}