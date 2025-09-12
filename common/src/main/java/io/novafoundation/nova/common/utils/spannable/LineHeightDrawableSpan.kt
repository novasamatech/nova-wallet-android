package io.novafoundation.nova.common.utils.spannable

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ReplacementSpan
import androidx.core.graphics.drawable.updateBounds
import kotlin.math.roundToInt

/**
 * Extends drawable height to line height without keeping aspect ratio
 */
class LineHeightDrawableSpan(
    private val drawable: Drawable
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val pFm = paint.fontMetricsInt
        fm?.apply {
            ascent = pFm.ascent
            descent = pFm.descent
            top = pFm.top
            bottom = pFm.bottom
        }

        return drawable.intrinsicWidth
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val lineHeight = (bottom - top).coerceAtLeast(1)
        val dH = drawable.intrinsicHeight.coerceAtLeast(1)

        val scale = lineHeight.toFloat() / dH
        val drawH = (dH * scale).roundToInt()

        drawable.updateBounds(bottom = drawH)

        val save = canvas.save()
        val transY = bottom - drawH
        canvas.translate(x, transY.toFloat())
        drawable.draw(canvas)
        canvas.restoreToCount(save)
    }
}
