package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.content.Context
import android.graphics.DashPathEffect
import io.novafoundation.nova.common.utils.dpF

class ChartUIParams(
    val gridLineWidthDp: Float,
    val chartLineWidthDp: Float,
    val gridLineDashEffect: DashPathEffect,
    val gridLines: Int
) {
    companion object {
        fun default(context: Context) = ChartUIParams(
            gridLineWidthDp = 1.5f,
            chartLineWidthDp = 1.5f,
            gridLineDashEffect = DashPathEffect(floatArrayOf(3f.dpF(context), 3f.dpF(context)), 0f),
            gridLines = 4
        )
    }
}
