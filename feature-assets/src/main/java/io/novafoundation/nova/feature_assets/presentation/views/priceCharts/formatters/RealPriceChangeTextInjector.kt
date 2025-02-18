package io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters

import android.widget.TextView
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency

interface PriceChangeTextInjector {
    fun format(fromValue: Float, toValue: Float, textView: TextView)
}

class RealPriceChangeTextInjector(
    private val resourceManager: ResourceManager,
    private val currency: Currency
) : PriceChangeTextInjector {

    override fun format(fromValue: Float, toValue: Float, textView: TextView) {
        val change = toValue - fromValue
        val changeInPercent = if (fromValue != 0f) {
            change / fromValue
        } else {
            0f
        }

        textView.text = resourceManager.getString(
            R.string.price_chart_price_change,
            change.toBigDecimal().formatAsCurrency(currency),
            changeInPercent.toBigDecimal().formatAsPercentage()
        )

        if (change < 0f) {
            textView.setTextColorRes(R.color.text_negative)
            textView.setDrawableStart(R.drawable.ic_arrow_down, tint = R.color.icon_negative, widthInDp = 16, paddingInDp = 2)
        } else {
            textView.setTextColorRes(R.color.text_positive)
            textView.setDrawableStart(R.drawable.ic_arrow_up, tint = R.color.icon_positive, widthInDp = 16, paddingInDp = 2)
        }
    }
}
