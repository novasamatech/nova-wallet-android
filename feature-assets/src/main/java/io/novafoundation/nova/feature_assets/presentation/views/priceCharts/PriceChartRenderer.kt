package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.datasets.ILineScatterCandleRadarDataSet
import com.github.mikephil.charting.renderer.LineChartRenderer
import io.novafoundation.nova.common.utils.dpF
import kotlin.math.roundToInt

/**
 * Draw a dot on selected entry
 */
class PriceChartRenderer(
    private val highlightColor: Int,
    private val dotRadius: Float,
    private val strokeWidth: Float,
    private val strokeAlpha: Float,
    private val chart: LineChart
) : LineChartRenderer(chart, chart.animator, chart.viewPortHandler) {

    private var dot: Entry? = null
    private var dotColor: Int? = null

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = highlightColor
        strokeWidth = 1.5f.dpF(chart.context)
        style = Paint.Style.STROKE
    }

    fun setDotPoint(entry: Entry?) {
        this.dot = entry
    }

    fun setDotColor(color: Int?) {
        this.dotColor = color
    }

    override fun drawHighlightLines(canvas: Canvas, x: Float, y: Float, set: ILineScatterCandleRadarDataSet<*>?) {
        canvas.drawLine(x, 0f, x, chart.height.toFloat(), linePaint)
    }

    override fun drawExtras(c: Canvas) {
        super.drawExtras(c)
        dot?.let {
            val point = it.toCanvasPoint()
            dotPaint.color = getAlphaWithArgb(dotColor ?: Color.WHITE, strokeAlpha)
            c.drawCircle(point.x, point.y, dotRadius + strokeWidth, dotPaint)
            dotPaint.color = dotColor ?: Color.WHITE
            c.drawCircle(point.x, point.y, dotRadius, dotPaint)
        }
    }

    private fun Entry.toCanvasPoint(): PointF {
        val transformer = chart.getTransformer(YAxis.AxisDependency.RIGHT)
        val values = floatArrayOf(x, y)
        transformer.pointValuesToPixel(values)
        return PointF(values[0], values[1])
    }

    fun getAlphaWithArgb(color: Int, alpha: Float): Int {
        return Color.argb((255 * alpha).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))
    }
}
