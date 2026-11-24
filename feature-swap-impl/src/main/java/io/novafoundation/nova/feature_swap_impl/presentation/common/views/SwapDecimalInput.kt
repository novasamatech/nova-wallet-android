package io.novafoundation.nova.feature_swap_impl.presentation.common.views

import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.DigitsKeyListener
import android.widget.EditText

// Hardware key events go through DigitsKeyListener before the edit filter can normalize them.
private const val SWAP_DECIMAL_KEYS = "0123456789.,"

internal fun EditText.setupSwapDecimalInput(maxFractionDigits: Int? = null) {
    keyListener = DigitsKeyListener.getInstance(SWAP_DECIMAL_KEYS)
    setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    filters += SwapDecimalInputFilter(maxFractionDigits)
}

internal class SwapDecimalInputFilter(private val maxFractionDigits: Int?) : InputFilter {

    private val decimalPattern = Regex("""[0-9]*(?:\.[0-9]*)?""")

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        if (start == end) return null

        val inserted = source.subSequence(start, end).toString()
        val normalized = inserted.replace(',', '.')

        val result = dest.toString().replaceRange(dstart, dend, normalized)
        val separator = result.indexOf('.')
        val exceedsPrecision = maxFractionDigits != null && separator >= 0 && result.lastIndex - separator > maxFractionDigits
        if (!decimalPattern.matches(result) || exceedsPrecision) return dest.subSequence(dstart, dend)

        if (normalized == inserted) return null
        if (source !is Spanned) return normalized

        return SpannableString(normalized).also { TextUtils.copySpansFrom(source, start, end, null, it, 0) }
    }
}
