package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.isNegative
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWeeklyPriceSummary
import java.math.BigDecimal
import java.math.RoundingMode

/** A non-interactive 40x20dp preview; full-screen chart controls do not belong in a list row. */
class BittensorSubnetSparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val path = Path()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.dpF(context)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var points = emptyList<Float>()

    fun setSummary(summary: BittensorWeeklyPriceSummary?) {
        points = normalizedBittensorSparkline(summary?.sparkline.orEmpty())
        paint.color = context.getColor(if (summary?.changePercent?.isNegative == true) R.color.text_negative else R.color.text_positive)
        rebuildPath()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPath()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    private fun rebuildPath() {
        path.reset()
        if (points.size < 2) return
        val inset = paint.strokeWidth / 2
        val availableWidth = (width - paint.strokeWidth).coerceAtLeast(0f)
        val availableHeight = (height - paint.strokeWidth).coerceAtLeast(0f)
        points.forEachIndexed { index, value ->
            val x = inset + availableWidth * index / (points.size - 1)
            val y = inset + availableHeight * (1 - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }
}

internal fun normalizedBittensorSparkline(points: List<BigDecimal>): List<Float> {
    if (points.size < 2) return emptyList()
    val minimum = points.minOrNull() ?: return emptyList()
    val maximum = points.maxOrNull() ?: return emptyList()
    val range = maximum - minimum
    if (range.compareTo(BigDecimal.ZERO) == 0) return points.map { 0.5f }
    return points.map { (it - minimum).divide(range, 8, RoundingMode.HALF_EVEN).toFloat() }
}
