package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.renderer.YAxisRenderer

/**
 * Draws the right axis [LimitLine]s with a readable label chip.
 *
 * MPAndroidChart paints the limit label as bare text directly on top of the dashed line, which is
 * unreadable against the plot. This renderer interrupts the line under the label and places the label on a
 * rounded background above the line at the right edge of the plot (below it when there is no room above).
 * The label is the line's own text; the caller decides whether it carries the amount.
 */
class ReferenceLineRenderer(
    chart: LineChart,
    private val style: Style
) : YAxisRenderer(chart.viewPortHandler, chart.axisRight, chart.getTransformer(YAxis.AxisDependency.RIGHT)) {

    class Style(
        @ColorInt val textColor: Int,
        val textSizePx: Float,
        val typeface: Typeface?,
        @ColorInt val chipColor: Int,
        val chipCornerRadiusPx: Float,
        val chipPaddingHorizontalPx: Float,
        val chipPaddingVerticalPx: Float,
        /** Gap between the chip and both the line and the plot edge. */
        val chipMarginPx: Float
    )

    private val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = style.chipColor
        it.style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = style.textColor
        it.textSize = style.textSizePx
        it.typeface = style.typeface
    }

    private val linePath = Path()
    private val chipBounds = RectF()
    private val pixelPosition = FloatArray(2)

    override fun renderLimitLines(c: Canvas) {
        val limitLines = mYAxis.limitLines
        if (limitLines.isNullOrEmpty()) return

        val content = mViewPortHandler.contentRect
        for (line in limitLines) {
            if (!line.isEnabled) continue

            pixelPosition[0] = 0f
            pixelPosition[1] = line.limit
            mTrans.pointValuesToPixel(pixelPosition)
            val y = pixelPosition[1]
            if (y < content.top || y > content.bottom) continue

            val saved = c.save()
            c.clipRect(content)
            val label = line.label.orEmpty()
            if (label.isBlank()) {
                drawLine(c, line, y, content, gapStart = null, gapEnd = null)
            } else {
                measureChip(label, y, content)
                drawLine(c, line, y, content, chipBounds.left - style.chipMarginPx, chipBounds.right + style.chipMarginPx)
                drawChip(c, label)
            }
            c.restoreToCount(saved)
        }
    }

    private fun measureChip(label: String, lineY: Float, content: RectF) {
        val metrics = textPaint.fontMetrics
        val width = textPaint.measureText(label) + 2 * style.chipPaddingHorizontalPx
        val height = metrics.descent - metrics.ascent + 2 * style.chipPaddingVerticalPx

        chipBounds.right = content.right - style.chipMarginPx
        chipBounds.left = chipBounds.right - width
        chipBounds.bottom = lineY - style.chipMarginPx
        chipBounds.top = chipBounds.bottom - height
        if (chipBounds.top < content.top) {
            chipBounds.top = lineY + style.chipMarginPx
            chipBounds.bottom = chipBounds.top + height
        }
    }

    /** Draws the dashed line across the plot, leaving the [gapStart]..[gapEnd] stretch under the chip empty. */
    private fun drawLine(c: Canvas, line: LimitLine, y: Float, content: RectF, gapStart: Float?, gapEnd: Float?) {
        mLimitLinePaint.style = Paint.Style.STROKE
        mLimitLinePaint.color = line.lineColor
        mLimitLinePaint.strokeWidth = line.lineWidth
        mLimitLinePaint.pathEffect = line.dashPathEffect

        linePath.reset()
        linePath.moveTo(content.left, y)
        linePath.lineTo((gapStart ?: content.right).coerceIn(content.left, content.right), y)
        if (gapEnd != null && gapEnd < content.right) {
            linePath.moveTo(gapEnd, y)
            linePath.lineTo(content.right, y)
        }
        c.drawPath(linePath, mLimitLinePaint)
    }

    private fun drawChip(c: Canvas, label: String) {
        c.drawRoundRect(chipBounds, style.chipCornerRadiusPx, style.chipCornerRadiusPx, chipPaint)
        val baseline = chipBounds.top + style.chipPaddingVerticalPx - textPaint.fontMetrics.ascent
        c.drawText(label, chipBounds.left + style.chipPaddingHorizontalPx, baseline, textPaint)
    }
}
