package io.novafoundation.nova.common.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.resources.ResourceManager

class SpannableStyler(val content: String) {

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

fun styleText(content: String, block: SpannableStyler.() -> Unit): Spannable {
    val builder = SpannableStyler(content)

    builder.block()

    return builder.build()
}

class SpannableBuilder(private val resourceManager: ResourceManager) {

    private val builder = SpannableStringBuilder()

    fun appendColored(text: String, @ColorRes color: Int): SpannableBuilder {
        val span = ForegroundColorSpan(resourceManager.getColor(color))

        return append(text, span)
    }

    fun append(text: String): SpannableBuilder {
        builder.append(text)

        return this
    }

    fun build(): SpannableString = SpannableString(builder)

    private fun append(text: String, span: Any): SpannableBuilder {
        builder.append(text, span, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        return this
    }
}

fun buildSpannable(resourceManager: ResourceManager, block: SpannableBuilder.() -> Unit): Spannable {
    val builder = SpannableBuilder(resourceManager).apply(block)

    return builder.build()
}
