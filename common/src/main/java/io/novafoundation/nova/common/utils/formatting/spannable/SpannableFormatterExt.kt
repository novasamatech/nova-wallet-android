package io.novafoundation.nova.common.utils.formatting.spannable

fun CharSequence.spannableFormatting(vararg args: Any): CharSequence {
    return SpannableFormatter.format(this, *args)
}
