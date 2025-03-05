package io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters

import android.widget.TextView
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import java.math.BigDecimal

interface PriceTextInjector {
    fun format(value: Float, textView: TextView, isEntrySelected: Boolean)
}

class RealPricePriceTextInjector(
    private val currency: Currency,
    private val lastCoinRate: BigDecimal?
) : PriceTextInjector {

    override fun format(value: Float, textView: TextView, isEntrySelected: Boolean) {
        textView.text = if (isEntrySelected || lastCoinRate == null) {
            value.toBigDecimal().formatAsCurrency(currency)
        } else {
            lastCoinRate.formatAsCurrency(currency)
        }
    }
}
