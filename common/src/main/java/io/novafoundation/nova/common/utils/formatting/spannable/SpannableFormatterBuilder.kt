package io.novafoundation.nova.common.utils.formatting.spannable

import android.text.SpannableStringBuilder

class SpannableFormatterBuilder(override val format: CharSequence) : SpannableFormatter.Builder {

    private val spannableStringBuilder = SpannableStringBuilder(format)

    override fun replace(start: Int, end: Int, text: CharSequence) {
        spannableStringBuilder.replace(start, end, text)
    }

    override fun result(): CharSequence {
        return spannableStringBuilder
    }
}
