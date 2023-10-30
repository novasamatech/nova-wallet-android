package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SwapInputMixinPriceImpactFiatFormatterFactory(
    private val priceImpactFormatter: PriceImpactFormatter,
) {

    fun create(priceImpactFlow: Flow<Percent?>): AmountChooserMixinBase.FiatFormatter {
        return SwapInputMixinPriceImpactFiatFormatter(priceImpactFormatter, priceImpactFlow)
    }
}

class SwapInputMixinPriceImpactFiatFormatter(
    private val priceImpactFormatter: PriceImpactFormatter,
    private val priceImpactFlow: Flow<Percent?>,
) : AmountChooserMixinBase.FiatFormatter {

    override fun formatFlow(tokenFlow: Flow<Token>, amountFlow: Flow<BigDecimal>): Flow<CharSequence> {
        return combine(tokenFlow, amountFlow, priceImpactFlow) { token, amount, priceImpact ->
            val formattedFiatAmount = token.amountToFiat(amount).formatAsCurrency(token.currency)

            priceImpactFormatter.formatWithBrackets(priceImpact)?.let {
                SpannableStringBuilder().apply {
                    append("$formattedFiatAmount ")
                    append(it)
                }
            } ?: formattedFiatAmount
        }
    }
}
