package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.view.MotionEvent
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener

class PriceChartGestureListener(
    private val onGestureStart: (MotionEvent) -> Unit,
    private val onGestureEnd: (MotionEvent) -> Unit
) : OnChartGestureListener {
    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
        onGestureStart(me)
    }

    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
        onGestureEnd(me)
    }

    override fun onChartLongPressed(me: MotionEvent?) {}

    override fun onChartDoubleTapped(me: MotionEvent?) {}

    override fun onChartSingleTapped(me: MotionEvent?) {}

    override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}

    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}

    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
}
