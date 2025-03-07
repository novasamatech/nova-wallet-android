package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent

class LongClickDetector(context: Context, private val onLongClickDetected: (e: MotionEvent) -> Unit) : GestureDetector.OnGestureListener {

    private val gestureDetector = GestureDetector(context, this)

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onLongPress(e: MotionEvent) {
        onLongClickDetected(e)
    }

    override fun onDown(e: MotionEvent): Boolean = false
    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
}
