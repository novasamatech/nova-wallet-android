package io.novafoundation.nova.common.view.banner.switcher.animation

import android.view.View
import android.view.animation.Interpolator
import io.novafoundation.nova.common.utils.multiplier
import io.novafoundation.nova.common.utils.recyclerView.expandable.flippedFraction
import kotlin.math.absoluteValue

interface FractionAnimator {

    fun animate(view: View, fraction: Float)
}

abstract class InterpolatedAnimator(
    private val interpolator: Interpolator
) : FractionAnimator {

    override fun animate(view: View, fraction: Float) {
        val interpolatedValue = interpolator.getInterpolation(fraction.absoluteValue) * fraction.multiplier()
        animateInternal(view, interpolatedValue)
    }

    protected abstract fun animateInternal(view: View, fraction: Float)
}

class InterpolationRange(val from: Float, val to: Float) {

    fun getValueFor(fraction: Float): Float {
        return from + (to - from) * fraction.absoluteValue
    }
}
