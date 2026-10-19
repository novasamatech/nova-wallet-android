package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.widget.TextView
import io.novafoundation.nova.common.utils.isNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.PriceChangeTextInjector
import java.math.BigDecimal
import java.text.NumberFormat

class BittensorChartChangeTextInjector(
    private val period: String,
    private val hasPeriodCoverage: Boolean = true
) : PriceChangeTextInjector {

    override fun format(fromValue: Float, toValue: Float, textView: TextView) {
        val change = if (hasPeriodCoverage && fromValue > 0f) (toValue - fromValue) / fromValue * 100 else null
        formatBittensorChartChange(change?.takeIf { it.isFinite() }?.toBigDecimal(), period, textView)
    }
}

class BittensorPortfolioChangeTextInjector(private val change: BigDecimal?, private val period: String) : PriceChangeTextInjector {

    override fun format(fromValue: Float, toValue: Float, textView: TextView) {
        format(textView)
    }

    fun format(textView: TextView) {
        formatBittensorChartChange(change?.movePointRight(2), period, textView)
    }
}

private fun formatBittensorChartChange(change: BigDecimal?, period: String, textView: TextView) {
    val formatted = change?.let {
        val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it)
        if (it.isPositive) "+$number" else number
    }
    textView.text = if (formatted != null) "$formatted% · $period" else ""
    textView.setTextColorRes(
        when {
            change == null -> R.color.text_secondary
            change.isNegative -> R.color.text_negative
            else -> R.color.text_positive
        }
    )
}
