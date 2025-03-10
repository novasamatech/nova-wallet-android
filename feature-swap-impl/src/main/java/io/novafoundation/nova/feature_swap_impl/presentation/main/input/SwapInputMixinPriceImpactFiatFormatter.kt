package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

class SwapInputMixinPriceImpactFiatFormatterFactory(
    private val priceImpactFormatter: PriceImpactFormatter,
) {

    fun create(priceImpactFlow: Flow<Fraction?>): AmountChooserMixinBase.FiatFormatter {
        return SwapInputMixinPriceImpactFiatFormatter(priceImpactFormatter, priceImpactFlow)
    }
}

class SwapInputMixinPriceImpactFiatFormatter(
    private val priceImpactFormatter: PriceImpactFormatter,
    private val priceImpactFlow: Flow<Fraction?>,
) : AmountChooserMixinBase.FiatFormatter {

    override fun formatFlow(tokenFlow: Flow<Token>, amountFlow: Flow<BigDecimal>): Flow<CharSequence> {
        return combine(tokenFlow, amountFlow, priceImpactFlow) { token, amount, priceImpact ->
            val formattedFiatAmount = token.amountToFiat(amount).formatAsCurrency(token.currency)
            val formattedPriceImpact = priceImpact?.let(priceImpactFormatter::formatWithBrackets)

            if (formattedPriceImpact != null) {
                SpannableStringBuilder().apply {
                    append("$formattedFiatAmount ")
                    append(formattedPriceImpact)
                }
            } else {
                formattedFiatAmount
            }
        }
    }
}
