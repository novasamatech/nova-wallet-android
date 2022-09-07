package io.novafoundation.nova.common.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.annotation.ColorInt

private fun clickableSpan(onClick: () -> Unit) = object : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }

    override fun onClick(widget: View) {
        onClick()
    }
}

private fun colorSpan(color: Int) = ForegroundColorSpan(color)

class SpannableBuilder(val content: String) {

    private val buildingSpannable = SpannableString(content)

    fun clickable(
        text: String,
        @ColorInt color: Int? = null,
        onClick: () -> Unit
    ) {
        val startIndex = content.indexOf(text)

        if (startIndex == -1) {
            return
        }

        val endIndex = startIndex + text.length

        buildingSpannable.setSpan(clickableSpan(onClick), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        color?.let {
            buildingSpannable.setSpan(colorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun build() = buildingSpannable
}

fun createSpannable(content: String, block: SpannableBuilder.() -> Unit): Spannable {
    val builder = SpannableBuilder(content)

    builder.block()

    return builder.build()
}
