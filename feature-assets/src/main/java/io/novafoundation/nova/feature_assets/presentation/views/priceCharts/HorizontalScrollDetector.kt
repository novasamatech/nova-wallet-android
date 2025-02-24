package io.novafoundation.nova.feature_assets.presentation.views.priceCharts

import android.view.MotionEvent
import kotlin.math.abs

class HorizontalScrollDetector(private val maxOffset: Float) {

    private var isHorizontalScroll = false
    private var startX = 0f

    fun isHorizontalScroll(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isHorizontalScroll) {
                    val offset = event.x - startX
                    isHorizontalScroll = abs(offset) > maxOffset
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isHorizontalScroll = false
            }
        }

        return isHorizontalScroll
    }
}
