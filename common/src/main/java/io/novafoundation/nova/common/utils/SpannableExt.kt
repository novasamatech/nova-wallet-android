package io.novafoundation.nova.common.utils

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.View
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.toSpannable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.spannable.LineHeightDrawableSpan

fun CharSequence.toSpannable(span: Any): Spannable {
    return this.toSpannable().setFullSpan(span)
}

fun CharSequence.bold(): Spannable {
    return toSpannable(boldSpan())
}

fun Spannable.setFullSpan(span: Any): Spannable {
    setSpan(span, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return this
}

// This method is nice for ImageSpan
fun Spannable.setEndSpan(span: Any): Spannable {
    return SpannableStringBuilder(this)
        .appendEnd(span)
}

fun SpannableStringBuilder.appendSpace(): SpannableStringBuilder {
    append(" ")
    return this
}

fun SpannableStringBuilder.append(text: CharSequence?, span: Any): SpannableStringBuilder {
    val startSpan = length
    append(text)
        .setSpan(span, startSpan, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return this
}

fun SpannableStringBuilder.appendEnd(span: Any): SpannableStringBuilder {
    appendSpace()
        .setSpan(span, length - 1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return this
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

fun fontSpan(resourceManager: ResourceManager, @FontRes fontRes: Int) = fontSpan(resourceManager.getFont(fontRes))

fun fontSpan(context: Context, @FontRes fontRes: Int) = fontSpan(ResourcesCompat.getFont(context, fontRes))

fun fontSpan(typeface: Typeface?): CharacterStyle {
    return when {
        typeface == null -> NoOpSpan()

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> typefaceSpanCompatV28(typeface)

        else -> CustomTypefaceSpan(typeface)
    }
}

fun boldSpan() = StyleSpan(Typeface.BOLD)

fun drawableText(drawable: Drawable, extendToLineHeight: Boolean = false): Spannable = SpannableStringBuilder().appendEnd(drawableSpan(drawable, extendToLineHeight))

fun drawableSpan(drawable: Drawable, extendToLineHeight: Boolean = false) = when (extendToLineHeight) {
    true -> LineHeightDrawableSpan(drawable)
    false -> ImageSpan(drawable)
}

fun CharSequence.formatAsSpannable(vararg args: Any): SpannedString {
    return SpannableFormatter.format(this, *args)
}

@TargetApi(Build.VERSION_CODES.P)
private fun typefaceSpanCompatV28(typeface: Typeface) =
    TypefaceSpan(typeface)

private class CustomTypefaceSpan(private val typeface: Typeface?) : MetricAffectingSpan() {
    override fun updateDrawState(paint: TextPaint) {
        paint.typeface = typeface
    }

    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = typeface
    }
}

private class NoOpSpan : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint?) {}
}

fun CharSequence.addColor(color: Int): Spannable {
    return this.toSpannable(colorSpan(color))
}
