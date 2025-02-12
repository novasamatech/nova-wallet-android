package io.novafoundation.nova.common.utils

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class ViewClickGestureDetector(view: View) : GestureDetector(view.context, ViewClickGestureDetectorListener(view))

private class ViewClickGestureDetectorListener(private val view: View) : GestureDetector.OnGestureListener {

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        view.performClick()
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

        return false
    }
}
