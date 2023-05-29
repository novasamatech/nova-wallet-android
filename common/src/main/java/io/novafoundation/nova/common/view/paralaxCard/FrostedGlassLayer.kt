package io.novafoundation.nova.common.view.paralaxCard

import android.graphics.Paint
import android.graphics.Path
import android.view.View

class FrostedGlassLayer {
    val layers: MutableList<ViewWithLayoutParams> = mutableListOf()
}

class ViewWithLayoutParams(val view: View, layoutParams: ParalaxCardView.LayoutParams) {
    val cardRadius = layoutParams.cardRadius

    val borderPath = Path()

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = layoutParams.cardBackgroundColor!!
    }

}
