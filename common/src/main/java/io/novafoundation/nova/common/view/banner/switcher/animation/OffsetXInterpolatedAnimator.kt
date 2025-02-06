package io.novafoundation.nova.common.view.banner.switcher.animation

import android.view.View
import android.view.animation.Interpolator

class OffsetXInterpolatedAnimator(
    interpolator: Interpolator,
    private val offsetRange: InterpolationRange,
) : InterpolatedAnimator(interpolator) {

    override fun animateInternal(view: View, fraction: Float) {
        view.translationX = offsetRange.getValueFor(fraction)
    }
}
