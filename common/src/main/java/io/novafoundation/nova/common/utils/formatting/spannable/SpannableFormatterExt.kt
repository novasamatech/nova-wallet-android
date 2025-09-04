package io.novafoundation.nova.common.utils.formatting.spannable

import android.content.Context
import android.text.SpannedString
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.toSpannable

fun CharSequence.spannableFormatting(vararg args: Any): CharSequence {
    return SpannableFormatter.format(this, *args)
}

fun SpannableFormatter.format(resourceManager: ResourceManager, @StringRes resId: Int, vararg args: Any): SpannedString {
    val format = resourceManager.getString(resId)
    return format(format, *args)
}

fun SpannableFormatter.format(context: Context, @StringRes resId: Int, vararg args: Any): SpannedString {
    val format = context.getString(resId)
    return format(format, *args)
}

fun Context.highlightedText(mainRes: Int, vararg highlightedRes: Int): SpannedString {
    val highlighted = highlightedRes.map {
        getString(it).toSpannable(colorSpan(getColor(R.color.text_primary)))
    }.toTypedArray()

    return SpannableFormatter.format(this, mainRes, *highlighted)
}

fun ResourceManager.highlightedText(mainRes: Int, vararg highlightedRes: Int): SpannedString {
    val highlighted = highlightedRes.map {
        getString(it).toSpannable(colorSpan(getColor(R.color.text_primary)))
    }.toTypedArray()

    return SpannableFormatter.format(this, mainRes, *highlighted)
}
