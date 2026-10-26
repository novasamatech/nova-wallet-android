package io.novafoundation.nova.feature_swap_impl.presentation.options

import android.text.InputFilter
import android.text.Spanned

/** iOS caps a custom slippage at four decimal places of a percent (e.g. 0.0125 %). */
const val SLIPPAGE_MAX_FRACTION_DIGITS = 4

internal fun slippageInputWithinPrecision(text: CharSequence, maxFractionDigits: Int): Boolean {
    val separator = text.indexOfFirst { it == '.' || it == ',' }
    return separator < 0 || text.length - separator - 1 <= maxFractionDigits
}

/** Rejects an edit that would leave more than [maxFractionDigits] digits after the decimal separator. */
class SlippagePrecisionInputFilter(private val maxFractionDigits: Int = SLIPPAGE_MAX_FRACTION_DIGITS) : InputFilter {

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val result = StringBuilder(dest).replace(dstart, dend, source.subSequence(start, end).toString())
        return if (slippageInputWithinPrecision(result, maxFractionDigits)) null else ""
    }
}
