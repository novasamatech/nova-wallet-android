package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.graphics.Path
import android.graphics.Paint.Style
import android.view.View
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_assets.R
import kotlin.math.sin

class ChartsShimmeringView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val chartUIParams = getSharedChartUIParams(context)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            color = context.getColor(R.color.neutral_price_chart_line)
            strokeWidth = chartUIParams.chartLineWidthDp.dpF(context)
            style = Style.STROKE
        }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            color = context.getColor(R.color.price_chart_grid_line)
            strokeWidth = chartUIParams.gridLineWidthDp.dpF(context)
            style = Style.STROKE
            pathEffect = chartUIParams.gridLineDashEffect
        }

    private val sinPath = Path()
    private val linePoints = 200

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        sinPath.reset()
        val periods = 2
        val chartZone = measuredHeight.toFloat() - paddingTop - paddingBottom
        val amplitude = (chartZone - linePaint.strokeWidth) / 2
        val centerY = paddingTop + chartZone / 2
        val pointStep = measuredWidth.toFloat() / linePoints.toFloat()
        for (point in 0..linePoints) {
            val x = point * pointStep
            val y = centerY + amplitude * sin(periods * Math.PI * 2 * x / measuredWidth).toFloat()
            if (sinPath.isEmpty)
                sinPath.moveTo(x, y)
            else
                sinPath.lineTo(x, y)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val gapsCountBetweenGrid = chartUIParams.gridLines - 1
        val drawingZone = height.toFloat() - paddingTop - paddingBottom
        repeat(chartUIParams.gridLines) { index ->
            val y = index * drawingZone / gapsCountBetweenGrid.toFloat() + paddingTop
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        canvas.drawPath(sinPath, linePaint)
    }
}
