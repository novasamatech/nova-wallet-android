package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.widget.TextView
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.PriceChangeTextInjector
import java.text.NumberFormat

class BittensorChartChangeTextInjector(private val period: String) : PriceChangeTextInjector {

    override fun format(fromValue: Float, toValue: Float, textView: TextView) {
        val change = if (fromValue > 0f) (toValue - fromValue) / fromValue * 100 else null
        val formatted = change?.let {
            val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it)
            if (it > 0) "+$number" else number
        }
        textView.text = if (formatted != null) "$formatted% · $period" else "— · $period"
        textView.setTextColorRes(
            when {
                change == null -> R.color.text_secondary
                change < 0f -> R.color.text_negative
                else -> R.color.text_positive
            }
        )
    }
}
