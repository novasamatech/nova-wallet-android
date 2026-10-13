package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.widget.TextView
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.PriceChangeTextInjector
import java.text.NumberFormat
import java.util.Locale

private const val HIDDEN_CHANGE = "\u2014"

/** Change of the portfolio value between the first and the selected point; hidden when the history does not cover the period. */
class BittensorChartChangeTextInjector(private val period: String, private val covered: Boolean) : PriceChangeTextInjector {

    override fun format(fromValue: Float, toValue: Float, textView: TextView) {
        val text = bittensorChartChangeText(fromValue, toValue, period, covered)
        textView.text = text
        textView.setTextColorRes(
            when {
                text.startsWith(HIDDEN_CHANGE) -> R.color.text_secondary
                toValue < fromValue -> R.color.text_negative
                else -> R.color.text_positive
            }
        )
    }
}

internal fun bittensorChartChangeText(fromValue: Float, toValue: Float, period: String, covered: Boolean, locale: Locale = Locale.getDefault()): String {
    if (!covered || fromValue <= 0f) return "$HIDDEN_CHANGE \u00b7 $period"

    val change = (toValue - fromValue) / fromValue * 100
    val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(change)
    val signed = if (change > 0f) "+$number" else number

    return "$signed% \u00b7 $period"
}
