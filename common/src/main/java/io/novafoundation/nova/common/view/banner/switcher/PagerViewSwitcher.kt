package io.novafoundation.nova.common.view.banner.switcher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class PagerViewSwitcher @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Controller<P, V : View> {

        fun getViews(): List<V>

        fun setAnimationState(animationOffset: Float, currentPayload: P, nextPayload: P)

        fun setState(payload: P)
    }

    fun interface InterpolatedAnimator {
        fun animate(view: View, fraction: Float)
    }

    sealed interface State {
        class Idle(val view: View) : State

        class InProgress(val inView: View, val outView: View) : State
    }

    private var controller: Controller<*, *>? = null
    private var animatedFraction: Float = 0f
    private var inAnimator: InterpolatedAnimator? = null
    private var outAnimator: InterpolatedAnimator? = null
    private var currentState: State? = null
    private var items: Int = 0

    fun setController(controller: Controller<*, *>) {
        this.controller = controller
        this.controller?.getViews()?.let { views ->
            views.forEach { addView(it) }
        }
    }

    fun setSize(size: Int) {

    }

    fun setAnimatedFraction(fraction: Float) {
        if (controller == null) return

        animatedFraction = fraction
    }

    fun setInAnimator(animator: InterpolatedAnimator) {
        inAnimator = animator
    }

    fun setOutAnimator(animator: InterpolatedAnimator) {
        outAnimator = animator
    }
}
