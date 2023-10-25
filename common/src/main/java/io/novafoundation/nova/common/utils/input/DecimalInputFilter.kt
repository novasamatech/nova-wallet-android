package io.novafoundation.nova.common.utils.input

import android.text.InputFilter
import android.text.Spanned

class DecimalInputFilter : InputFilter {

    val regex = Regex("[^0-9.]")
    val dotRegex = "\\.".toRegex()

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
        var result: String = source.replace(regex, "")

        if (dest.isEmpty() || dest.contains('.')) {
            result = result.replace(dotRegex, "")
        }

        return result
    }
}
