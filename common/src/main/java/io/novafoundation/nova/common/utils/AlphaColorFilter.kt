package io.novafoundation.nova.common.utils

import android.graphics.ColorMatrixColorFilter

class AlphaColorFilter(val alpha: Float) : ColorMatrixColorFilter(
    floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, alpha, 0f
    )
)
