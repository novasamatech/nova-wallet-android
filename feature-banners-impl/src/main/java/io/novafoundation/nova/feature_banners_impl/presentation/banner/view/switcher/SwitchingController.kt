package io.novafoundation.nova.feature_banners_impl.presentation.banner.view.switcher

import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.feature_banners_impl.presentation.banner.view.switcher.animation.FractionAnimator


abstract class SwitchingController<P, V : View>(
    private var inAnimator: FractionAnimator,
    private var outAnimator: FractionAnimator
) {

    abstract fun getViews(): Pair<V, V>

    fun setState(currentPayload: P, nextPayload: P) {
        val (currentView, nextView) = getViews()
        setViewPayload(currentView, currentPayload)
        setViewPayload(nextView, nextPayload)
    }

    fun setState(payload: P) {
        val (currentView, nextView) = getViews()
        setViewPayload(currentView, payload)
    }

    fun setAnimationState(animationOffset: Float) {
        val (currentView, nextView) = getViews()
        outAnimator.animate(currentView, animationOffset)
        inAnimator.animate(nextView, animationOffset)
    }

    fun setInAnimator(animator: FractionAnimator) {
        inAnimator = animator
    }

    fun setOutAnimator(animator: FractionAnimator) {
        outAnimator = animator
    }

    abstract fun setViewPayload(view: V, payload: P)
}

fun ViewGroup.setSwitchingController(controller: SwitchingController<*, *>) {
    controller.getViews().toList().forEach { addView(it) }
}
