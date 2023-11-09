package io.novafoundation.nova.common.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
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

    fun appendColored(text: String, @ColorRes color: Int) {
        val span = ForegroundColorSpan(resourceManager.getColor(color))

        append(text, span)
    }

    fun appendColored(@StringRes textRes: Int, @ColorRes color: Int) {
        val text = resourceManager.getString(textRes)

        return appendColored(text, color)
    }

    fun append(text: String) {
        builder.append(text)
    }

    fun setFullSpan(span: Any) {
        builder.setSpan(span, 0, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun appendSpan(span: Any) {
        builder.append(" ")
        builder.setSpan(span, builder.length - 1, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
