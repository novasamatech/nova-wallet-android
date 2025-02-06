package io.novafoundation.nova.common.view.banner.switcher.animation

import android.view.View
import android.view.animation.Interpolator

class AlphaInterpolatedAnimator(
    interpolator: Interpolator,
    val alphaRange: InterpolationRange
) : InterpolatedAnimator(interpolator) {

    override fun animateInternal(view: View, fraction: Float) {
        view.alpha = alphaRange.getValueFor(fraction)
    }
}
