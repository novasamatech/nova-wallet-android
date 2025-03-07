import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.minus

class LongPressDetector(
    private val cancelDistance: Float,
    private val timeout: Long,
    private val onLongPress: (MotionEvent) -> Unit
) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressTriggered = false
    private var lastMotionEvent: MotionEvent? = null
    private var startTouchPoint = PointF()

    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        lastMotionEvent?.let { onLongPress.invoke(it) }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        lastMotionEvent = MotionEvent.obtain(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startTouchPoint = PointF(event.x, event.y)
                isLongPressTriggered = false
                handler.postDelayed(longPressRunnable, timeout)
            }

            MotionEvent.ACTION_MOVE -> {
                val currentTouchPoint = PointF(event.x, event.y)
                val delta = currentTouchPoint.minus(startTouchPoint)
                if (!isLongPressTriggered && delta.length() > cancelDistance) {
                    cancelLongPress()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
            }
        }
        return true
    }

    private fun cancelLongPress() {
        handler.removeCallbacks(longPressRunnable)
        isLongPressTriggered = false
        lastMotionEvent = null
    }
}
