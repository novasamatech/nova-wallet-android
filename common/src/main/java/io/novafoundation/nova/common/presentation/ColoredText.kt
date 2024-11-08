package io.novafoundation.nova.common.presentation

import android.widget.TextView
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextColorRes

data class ColoredText(
    val text: CharSequence,
    @ColorRes val colorRes: Int,
)

fun TextView.setColoredText(coloredText: ColoredText) {
    text = coloredText.text
    setTextColorRes(coloredText.colorRes)
}

fun TextView.setColoredTextOrHide(coloredText: ColoredText?) = letOrHide(coloredText, ::setColoredText)
