package io.novafoundation.nova.feature_banners_api.presentation.view

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import io.novafoundation.nova.common.utils.dp
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

private const val MIN_FLING_VELOCITY = 1000 // px/s

enum class PageOffset(val scrollDirection: Int, val pageOffset: Int) {
    NEXT(-1, 1), PREVIOUS(1, -1), SAME(0, 0)
}

class BannerPagerScrollController(private val context: Context, private val callback: ScrollCallback) {

    interface ScrollCallback {
        fun onScrollToPage(pageOffset: Float, toPage: PageOffset)

        fun onScrollDirectionChanged(toPage: PageOffset)

        fun onScrollFinished(pageOffset: PageOffset)

        fun invalidateScroll()
    }

    private val scrollTracking: ScrollTracking = ScrollTracking()
    private var containerWidth = 0

    private var isTouchable: Boolean = true

    fun setTouchable(touchable: Boolean) {
        isTouchable = touchable
    }

    fun setContainerWidth(width: Int) {
        this.containerWidth = width
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isTouchable) return false

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scrollTracking.onStartScroll(context, event.x)
                scrollTracking.addMovement(event)
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val oldScrollDirection = scrollTracking.getPageOffset()

                scrollTracking.addMovement(event)
                scrollTracking.updateLastX(event.x)

                val newScrollDirection = scrollTracking.getPageOffset()

                if (oldScrollDirection == PageOffset.SAME || oldScrollDirection != newScrollDirection) {
                    callback.onScrollDirectionChanged(newScrollDirection)
                }

                callback.onScrollToPage(currentPageOffset(), newScrollDirection)

                val isHorizontalScroll = scrollTracking.eventDx().absoluteValue > 8.dp(context)
                isHorizontalScroll
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                scrollTracking.addMovement(event)

                val velocity = scrollTracking.getVelocity()
                handleSwipe(scrollTracking.currentScroll(), velocity)
                scrollTracking.recycle()
                true
            }

            else -> false
        }
    }

    fun computeScroll() {
        if (scrollTracking.computeScroll()) {
            val pageOffset = scrollTracking.getPageOffset()
            callback.onScrollToPage(currentPageOffset(), pageOffset)
            callback.invalidateScroll()

            if (scrollTracking.state == ScrollTracking.State.IDLE) {
                callback.onScrollFinished(pageOffset)
                scrollTracking.onFinishScroll()
            }
        }
    }

    private fun handleSwipe(dx: Float, velocityX: Float) {
        val isVelocityEnough = abs(velocityX) > MIN_FLING_VELOCITY
        val isSwipeEnough = abs(dx) > containerWidth / 4

        val shouldFling = isVelocityEnough || isSwipeEnough

        val pageOffset = when {
            dx < 0 && shouldFling -> PageOffset.NEXT
            dx > 0 && shouldFling -> PageOffset.PREVIOUS
            else -> PageOffset.SAME
        }

        smoothScrollToPage(pageOffset, velocityX)
    }

    private fun smoothScrollToPage(page: PageOffset, velocityX: Float) {
        val scrollWidth = page.scrollDirection * containerWidth
        val duration = computeScrollDuration(abs(velocityX))
        scrollTracking.smoothScrollToPosition(context, scrollWidth, duration)
        callback.invalidateScroll()
    }

    private fun computeScrollDuration(velocityX: Float): Int {
        val baseDuration = 200 // стандартное время анимации
        val maxDuration = 600 // максимальное время анимации
        return (baseDuration - min(velocityX / 2, baseDuration.toFloat())).toInt().coerceAtLeast(150).coerceAtMost(maxDuration)
    }

    private fun currentPageOffset(): Float {
        return try {
            val scrollOffset = scrollTracking.currentScroll() / containerWidth

            // We flip scroll to get page offset
            -scrollOffset.coerceIn(-1f, 1f)
        } catch (e: ArithmeticException) {
            0f
        }
    }

    fun swipeToPage(next: PageOffset) {
        callback.onScrollDirectionChanged(next)
        smoothScrollToPage(next, 0f)
    }

    fun isIdle(): Boolean {
        return scrollTracking.state == ScrollTracking.State.IDLE
    }
}

private class ScrollTracking {

    enum class State {
        IDLE, SCROLLING, SCROLLING_RELEASED
    }

    var state = State.IDLE
        private set

    private var currentScroll = 0f
    private var velocityTracker: VelocityTracker? = null
    private var scroller: Scroller? = null

    private var eventDownX = 0f
    private var eventLastX = 0f

    fun getVelocity(): Float {
        velocityTracker?.computeCurrentVelocity(MIN_FLING_VELOCITY)
        return velocityTracker?.xVelocity ?: return 0f
    }

    fun addMovement(event: MotionEvent) {
        velocityTracker?.addMovement(event)
    }

    fun updateLastX(x: Float) {
        val dx = x - eventLastX
        currentScroll += dx

        eventLastX = x
    }

    fun currentScroll(): Float {
        return currentScroll
    }

    fun eventDx(): Float {
        return eventLastX - eventDownX
    }

    fun computeScroll(): Boolean {
        val scroller = scroller ?: return false
        val scrollComputed = scroller.computeScrollOffset()

        currentScroll = scroller.currX.toFloat()

        if (scrollComputed && scroller.isFinished) { // Call only once when scroll is finished
            state = State.IDLE
        }

        return scrollComputed
    }

    fun onStartScroll(context: Context, eventX: Float) {
        scroller?.forceFinished(true)

        if (state == State.IDLE) {
            state = State.SCROLLING
            currentScroll = 0f
        }

        eventDownX = eventX
        eventLastX = eventX

        velocityTracker = VelocityTracker.obtain()
        createScroller(context)
    }

    fun onFinishScroll() {
        currentScroll = 0f
        scroller = null
        eventDownX = 0f
        eventLastX = 0f
    }

    fun recycle() {
        velocityTracker?.recycle()
    }

    fun smoothScrollToPosition(context: Context, toPosition: Int, duration: Int) {
        if (state == State.IDLE) {
            createScroller(context)
        }

        state = State.SCROLLING_RELEASED
        val currentPosition = currentScroll.toInt()
        scroller?.startScroll(currentPosition, 0, toPosition - currentPosition, 0, duration)
    }

    fun getPageOffset(): PageOffset {
        val currentScroll = currentScroll.toInt()

        return when {
            currentScroll < 0 -> PageOffset.NEXT
            currentScroll > 0 -> PageOffset.PREVIOUS
            else -> PageOffset.SAME
        }
    }

    private fun createScroller(context: Context) {
        scroller = Scroller(context, DecelerateInterpolator())
    }
}
