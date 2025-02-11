package io.novafoundation.nova.feature_banners_api.presentation.view

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dpF

private class Indicator(var size: Float, @ColorInt var color: Int)

class PageIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pagesCount = 0
    private val indicators = mutableListOf<Indicator>()

    private val inactiveIndicatorColor = context.getColor(R.color.icon_inactive)
    private val activeIndicatorColor = context.getColor(R.color.chip_icon)
    private val indicatorRadius = 3.dpF
    private val indicatorWidth = indicatorRadius * 2
    private val indicatorMargin = 6.dpF
    private val indicatorFullLength = 14.dpF

    private val argbEvaluator = ArgbEvaluator()

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            style = Paint.Style.STROKE
            strokeWidth = indicatorWidth
            strokeCap = Paint.Cap.ROUND
        }

    fun setPagesSize(size: Int) {
        pagesCount = size
        indicators.clear()
        if (size > 0) {
            indicators.addAll(List(size) { Indicator(0f, inactiveIndicatorColor) })
            indicators.first().apply {
                this.size = 1f
                this.color = activeIndicatorColor
            }
        }

        invalidate()
    }

    fun setCurrentPage(pageIndex: Int) {
        setAnimationProgress(1f, 0, pageIndex)
    }

    fun setAnimationProgress(offset: Float, fromPage: Int, toPage: Int) {
        val clippedOffset = offset.coerceIn(0f, 1f)

        indicators.forEachIndexed { index, value ->
            when (index) {
                fromPage -> {
                    value.size = 1f - clippedOffset
                    value.color = argbEvaluator.evaluate(clippedOffset, activeIndicatorColor, inactiveIndicatorColor) as Int
                }

                toPage -> {
                    value.size = clippedOffset
                    value.color = argbEvaluator.evaluate(clippedOffset, inactiveIndicatorColor, activeIndicatorColor) as Int
                }

                else -> {
                    value.size = 0f
                    value.color = inactiveIndicatorColor
                }
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val startPadding = indicatorRadius
        var lastEnd = -startPadding

        indicators.forEachIndexed { index, indicator ->
            indicatorPaint.color = indicator.color
            val start = lastEnd + indicatorMargin + indicatorWidth
            lastEnd = start + indicator.size * indicatorFullLength
            canvas.drawLine(
                start,
                height / 2f,
                lastEnd,
                height / 2f,
                indicatorPaint
            )
        }
    }
}
