package io.novafoundation.nova.common.view.parallaxCard

import android.graphics.Paint
import android.graphics.Path
import android.view.View

class FrostedGlassLayer {
    val layers: MutableList<ViewWithLayoutParams> = mutableListOf()
}

class ViewWithLayoutParams(val view: View, layoutParams: ParallaxCardView.LayoutParams) {
    val cardRadius = layoutParams.cardRadius

    val borderPath = Path()

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = layoutParams.cardBackgroundColor!!
    }

}
