package io.novafoundation.nova.feature_banners_api.presentation.view.switcher

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import io.novafoundation.nova.common.utils.removed
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation.FractionAnimator

class InOutAnimators(
    val inAnimator: FractionAnimator,
    val outAnimator: FractionAnimator
)

abstract class SwitchingController<P, V : View>(
    private val rightSwitchingAnimators: InOutAnimators,
    private val leftSwitchingAnimators: InOutAnimators
) {

    private var parent: ViewGroup? = null
    private var views: List<V> = listOf()

    protected abstract fun setPayloadsInternal(payloads: List<P>): List<V>

    fun setPayloads(payloads: List<P>): List<V> {
        views = setPayloadsInternal(payloads)
        setViewsToParent()
        return views
    }

    fun attachToParent(parent: ViewGroup) {
        this.parent = parent
        setViewsToParent()
    }

    fun setAnimationState(animationOffset: Float, from: Int, to: Int) {
        if (from >= views.size || to >= views.size) return

        if (from == to) {
            showPageImmediately(from)
        } else {
            val (currentView, nextView) = showPagesByIndex(from, to)
            val animators = when {
                animationOffset > 0 -> rightSwitchingAnimators
                else -> leftSwitchingAnimators
            }

            animators.outAnimator.animate(currentView, animationOffset)
            animators.inAnimator.animate(nextView, animationOffset)
        }
    }

    fun showPageImmediately(index: Int) {
        if (index >= views.size) return

        val (page) = showPagesByIndex(index)
        rightSwitchingAnimators.outAnimator.animate(page, 0f)
    }

    // Pay attention that after using this method removed view is still contains in its parents
    // We do it this way to have the same size of banners after remove a page
    fun removePageAt(pageToRemove: Int) {
        val removedView = views[pageToRemove]
        views = views.removed { removedView == it }
        removedView.isInvisible = true
    }

    private fun showPagesByIndex(vararg indexes: Int): List<V> {
        views.forEachIndexed { index, view ->
            view.setVisible(index in indexes, falseState = View.INVISIBLE)
        }

        return indexes.map { views[it] }
    }

    private fun setViewsToParent() {
        parent?.removeAllViews()
        views.forEach {
            parent?.addView(it)
        }
    }
}
