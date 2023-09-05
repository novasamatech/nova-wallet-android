package io.novafoundation.nova.common.utils

import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.View
import androidx.core.text.toSpannable

fun CharSequence.toSpannable(span: Any): Spannable {
    return this.toSpannable().setFullSpan(span)
}

fun Spannable.setFullSpan(span: Any): Spannable {
    setSpan(span, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return this
}

// This method is nice for ImageSpan
fun Spannable.setEndSpan(span: Any): Spannable {
    val spannable = SpannableStringBuilder(this)
    spannable.append(" ")
        .setSpan(span, length, length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}

fun clickableSpan(onClick: () -> Unit) = object : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }

    override fun onClick(widget: View) {
        onClick()
    }
}

fun colorSpan(color: Int) = ForegroundColorSpan(color)

fun drawableSpan(drawable: Drawable) = ImageSpan(drawable)

fun CharSequence.formatAsSpannable(vararg args: Any): SpannedString {
    return SpannableFormatter.format(this, *args)
}
