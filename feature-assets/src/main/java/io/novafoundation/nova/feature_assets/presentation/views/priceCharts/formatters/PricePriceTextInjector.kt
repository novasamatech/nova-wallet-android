package io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters

import android.widget.TextView
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency

interface PriceTextInjector {
    fun format(value: Float, textView: TextView)
}

class RealPricePriceTextInjector(private val currency: Currency) : PriceTextInjector {

    override fun format(value: Float, textView: TextView) {
        textView.text = value.toBigDecimal().formatAsCurrency(currency)
    }
}
