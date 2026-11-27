package io.novafoundation.nova.common.view.input

import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.DigitsKeyListener
import android.widget.EditText

// Hardware key events go through DigitsKeyListener before the edit filter can normalize them.
private const val DECIMAL_KEYS = "0123456789.,"

fun EditText.setupDecimalInput(constraints: DecimalInputConstraints = DecimalInputConstraints.Unlimited) {
    keyListener = DigitsKeyListener.getInstance(DECIMAL_KEYS)
    setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    setDecimalInputConstraints(constraints)
}

fun EditText.setDecimalInputConstraints(constraints: DecimalInputConstraints) {
    val otherFilters = filters.filterNot { it is DecimalInputFilter }
    filters = (otherFilters + DecimalInputFilter(constraints)).toTypedArray()
}

class DecimalInputFilter(private val constraints: DecimalInputConstraints) : InputFilter {

    private val decimalPattern = Regex("""[0-9]*(?:\.[0-9]*)?""")

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val inserted = source.subSequence(start, end).toString()
        val normalized = inserted.replace(',', '.')

        val original = dest.toString()
        val result = original.replaceRange(dstart, dend, normalized)
        if (!decimalPattern.matches(result) || exceedsConstraints(result, original)) return dest.subSequence(dstart, dend)

        if (normalized == inserted) return null
        if (source !is Spanned) return normalized

        return SpannableString(normalized).also { TextUtils.copySpansFrom(source, start, end, null, it, 0) }
    }

    private fun exceedsConstraints(result: String, original: String): Boolean {
        val isReducingInput = result.length < original.length
        return when (val constraints = constraints) {
            DecimalInputConstraints.Unlimited -> false
            is DecimalInputConstraints.FractionOnly ->
                exceedsLimit(result.fractionDigits(), original.fractionDigits(), constraints.maxFractionDigits, isReducingInput)
            is DecimalInputConstraints.Bounded ->
                exceedsLimit(result.fractionDigits(), original.fractionDigits(), constraints.maxFractionDigits, isReducingInput) ||
                    exceedsLimit(result.integerDigits(), original.integerDigits(), constraints.maxIntegerDigits, isReducingInput)
        }
    }

    // If constraints changed, let the user reduce existing excess digits without changing the amount for them.
    private fun exceedsLimit(newDigits: Int, oldDigits: Int, limit: Int, isReducingInput: Boolean): Boolean {
        return newDigits > limit && (newDigits > oldDigits || !isReducingInput)
    }

    private fun String.fractionDigits(): Int = substringAfter('.', "").length

    private fun String.integerDigits(): Int = substringBefore('.').trimStart('0').length.coerceAtLeast(1)
}
