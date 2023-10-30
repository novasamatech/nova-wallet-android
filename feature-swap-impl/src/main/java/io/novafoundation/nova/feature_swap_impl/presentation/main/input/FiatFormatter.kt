package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SwapInputMixinDefaultFiatFormatter : SwapAmountInputMixin.FiatFormatter {

    override fun formatFlow(assetFlow: Flow<Asset>, amountFlow: Flow<BigDecimal>): Flow<CharSequence> {
        return combine(assetFlow, amountFlow) { asset, amount ->
            asset.token.amountToFiat(amount).formatAsCurrency(asset.token.currency)
        }
    }
}

class SwapInputMixinPriceImpactFiatFormatterFactory(
    private val priceImpactFormatter: PriceImpactFormatter,
) {

    fun create(priceImpactFlow: Flow<Percent?>): SwapAmountInputMixin.FiatFormatter {
        return SwapInputMixinPriceImpactFiatFormatter(priceImpactFormatter, priceImpactFlow)
    }
}

class SwapInputMixinPriceImpactFiatFormatter(
    private val priceImpactFormatter: PriceImpactFormatter,
    private val priceImpactFlow: Flow<Percent?>,
) : SwapAmountInputMixin.FiatFormatter {

    override fun formatFlow(assetFlow: Flow<Asset>, amountFlow: Flow<BigDecimal>): Flow<CharSequence> {
        return combine(assetFlow, amountFlow, priceImpactFlow) { asset, amount, priceImpact ->
            val formattedFiatAmount = asset.token.amountToFiat(amount).formatAsCurrency(asset.token.currency)

            priceImpactFormatter.formatWithBrackets(priceImpact)?.let {
                SpannableStringBuilder().apply {
                    append("$formattedFiatAmount ")
                    append(it)
                }
            } ?: formattedFiatAmount
        }
    }
}
