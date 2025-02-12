package io.novafoundation.nova.feature_banners_api.presentation.view

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dpF

private const val NO_PAGE = -1

private class Indicator(var size: Float, var marginToNext: Float, @ColorInt var color: Int)

class PageIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pagesCount = 0
    private val indicators = mutableListOf<Indicator>()

    private val inactiveIndicatorColor = context.getColor(R.color.icon_inactive)
    private val activeIndicatorColor = context.getColor(R.color.chip_icon)
    private val goneIndicatorColor = Color.TRANSPARENT
    private val indicatorRadius = 3.dpF
    private val indicatorWidth = indicatorRadius * 2
    private val indicatorMargin = 12.dpF
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
            indicators.addAll(List(size) { Indicator(0f, indicatorMargin, inactiveIndicatorColor) })
            indicators.first().apply {
                this.size = indicatorFullLength
                this.color = activeIndicatorColor
            }
        }

        invalidate()
    }

    fun setCurrentPage(pageIndex: Int) {
        setAnimationProgress(1f, fromPage = NO_PAGE, toPage = pageIndex)
    }

    fun setAnimationProgress(offset: Float, fromPage: Int, toPage: Int) {
        setAnimation(offset.coerceIn(0f, 1f), fromPage, toPage, removeFrom = false)
    }

    fun setCloseProgress(offset: Float, closingPage: Int, nextPage: Int) {
        setAnimation(offset.coerceIn(0f, 1f), closingPage, nextPage, removeFrom = true)
    }

    private fun setAnimation(offset: Float, fromPage: Int, toPage: Int, removeFrom: Boolean) {
        if (indicators.size <= 1) {
            indicators.forEach { it.color = goneIndicatorColor }
            invalidate()
            return
        }

        if (fromPage >= indicators.size || toPage >= indicators.size) return

        indicators.forEachIndexed { index, indicator ->
            when (index) {
                fromPage -> {
                    indicator.size = indicatorFullLength - offset * indicatorFullLength

                    val marginToNext = if (removeFrom) indicatorMargin - offset * indicatorMargin else indicatorMargin
                    val endColor = if (removeFrom) goneIndicatorColor else inactiveIndicatorColor

                    indicator.marginToNext = marginToNext
                    indicator.color = argbEvaluator.evaluate(offset, activeIndicatorColor, endColor) as Int
                }

                toPage -> {
                    indicator.size = offset * indicatorFullLength
                    indicator.marginToNext = indicatorMargin
                    indicator.color = argbEvaluator.evaluate(offset, inactiveIndicatorColor, activeIndicatorColor) as Int
                }

                else -> {
                    indicator.size = 0f
                    indicator.marginToNext = indicatorMargin
                    indicator.color = inactiveIndicatorColor
                }
            }
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val startMargin = indicatorRadius
        var lastEnd = startMargin

        indicators.forEachIndexed { index, indicator ->
            indicatorPaint.color = indicator.color
            val start = lastEnd
            lastEnd = start + indicator.size
            canvas.drawLine(
                start,
                height / 2f,
                lastEnd,
                height / 2f,
                indicatorPaint
            )
            lastEnd += indicator.marginToNext
        }
    }
}
