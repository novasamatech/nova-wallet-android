package io.novafoundation.nova.common.view.paralaxCard

import android.graphics.Paint
import androidx.annotation.ColorInt

class ShadowData(
    @ColorInt val color: Int,
    val radius: Float
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color
        setShadowLayer(radius, 0f, 0f, color)
    }
}
