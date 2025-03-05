package io.novafoundation.nova.feature_banners_api.presentation.view.switcher.animation

import android.view.View

class CompoundInterpolatedAnimator(
    private val animators: List<FractionAnimator>
) : FractionAnimator {

    constructor(vararg animators: FractionAnimator) : this(animators.toList())

    override fun animate(view: View, fraction: Float) {
        animators.forEach { it.animate(view, fraction) }
    }
}
