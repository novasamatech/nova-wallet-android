package io.novafoundation.nova.common.presentation

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class ColoredDrawable(
    @DrawableRes val drawableRes: Int,
    @ColorRes val iconColor: Int
)
