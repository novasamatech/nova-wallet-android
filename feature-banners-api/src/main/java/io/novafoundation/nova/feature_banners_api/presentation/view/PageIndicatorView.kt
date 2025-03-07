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
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pagesCount = 0
    private val indicators = mutableListOf<Indicator>()

    private val indicatorColor = context.getColor(R.color.icon_inactive)
    private val goneIndicatorColor = Color.TRANSPARENT
    private val indicatorRadius = 3.dpF
    private val indicatorWidth = indicatorRadius * 2
    private val indicatorMargin = 12.dpF
    private val indicatorFullLength = 14.dpF

    private var indicatorsBoxWidth = 0f

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
            indicators.addAll(List(size) { Indicator(0f, indicatorMargin, indicatorColor) })
            selectIndicatorInstantly(0)
        }

        invalidate()
    }

    fun selectIndicatorInstantly(pageIndex: Int) {
        setAnimationProgress(1f, fromPage = NO_PAGE, toPage = pageIndex)
    }

    fun setAnimationProgress(offset: Float, fromPage: Int, toPage: Int) {
        setAnimationProgressInternal(offset.coerceIn(0f, 1f), fromPage, toPage, removeFrom = false)
    }

    fun setCloseAnimationProgress(offset: Float, closingPage: Int, nextPage: Int) {
        setAnimationProgressInternal(offset.coerceIn(0f, 1f), closingPage, nextPage, removeFrom = true)
    }

    private fun setAnimationProgressInternal(offset: Float, fromIndex: Int, toIndex: Int, removeFrom: Boolean) {
        if (indicators.size <= 1) {
            hideIndicators()
            invalidate()
            return
        }

        clearIndicatorSizeParams()
        increaseSizeForAnimationOffset(toIndex, offset)
        decreaseSizeForAnimationOffset(fromIndex, offset, removeFrom)

        calculateIndicatorsBoxWidth()
        invalidate()
    }

    private fun calculateIndicatorsBoxWidth() {
        var newIndicatorBoxWidth = 0f
        newIndicatorBoxWidth += indicatorRadius * 2 // Add start and end radius of indicator
        indicators.forEach {
            newIndicatorBoxWidth += it.size + it.marginToNext
        }
        indicatorsBoxWidth = newIndicatorBoxWidth
    }

    private fun increaseSizeForAnimationOffset(indicatorIndex: Int, offset: Float) {
        val indicator = indicators.getOrNull(indicatorIndex) ?: return
        indicator.size = offset * indicatorFullLength
        indicator.marginToNext = indicatorMargin
    }

    private fun decreaseSizeForAnimationOffset(indicatorIndex: Int, offset: Float, isRemovingAnimation: Boolean) {
        val indicator = indicators.getOrNull(indicatorIndex) ?: return

        indicator.size = indicatorFullLength - offset * indicatorFullLength

        val marginToNext = if (isRemovingAnimation) indicatorMargin - offset * indicatorMargin else indicatorMargin
        val endColor = if (isRemovingAnimation) goneIndicatorColor else indicatorColor

        indicator.marginToNext = marginToNext
        indicator.color = argbEvaluator.evaluate(offset, indicatorColor, endColor) as Int
    }

    private fun clearIndicatorSizeParams() {
        indicators.forEach {
            it.size = 0f
            it.marginToNext = indicatorMargin
        }
        indicators.lastOrNull()?.marginToNext = 0f
    }

    private fun hideIndicators() {
        indicators.forEach { it.color = goneIndicatorColor }
    }

    override fun onDraw(canvas: Canvas) {
        val startSpace = (measuredWidth - indicatorsBoxWidth) / 2
        val startMarginIndicator = indicatorRadius
        var lastEnd = startSpace + startMarginIndicator

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
