package io.novafoundation.nova.common.presentation

import androidx.annotation.ColorRes

data class ColoredText(
    val text: String,
    @ColorRes val colorRes: Int,
)
