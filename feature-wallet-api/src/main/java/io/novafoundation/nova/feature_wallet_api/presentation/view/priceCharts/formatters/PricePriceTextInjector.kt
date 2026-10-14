package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters

import android.widget.TextView
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import java.math.BigDecimal

interface PriceTextInjector {
    fun format(value: Float, textView: TextView, isEntrySelected: Boolean)
}

class RealPricePriceTextInjector(
    private val currency: Currency,
    private val lastCoinRate: BigDecimal?,
    private val fiatFormatter: FiatFormatter
) : PriceTextInjector {

    override fun format(value: Float, textView: TextView, isEntrySelected: Boolean) {
        val amount = if (isEntrySelected || lastCoinRate == null) value.toBigDecimal() else lastCoinRate
        textView.text = fiatFormatter.formatFiat(amount, currency)
    }
}
