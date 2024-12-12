package io.novafoundation.nova.common.utils

import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider

class RoundCornersOutlineProvider(
    private val cornerRadius: Float,
    private val margin: Rect = Rect()
) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(
            0 + margin.left,
            0 + margin.top,
            view.width - margin.right,
            view.height - margin.bottom,
            cornerRadius
        )
    }
}
