package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DefaultFiatFormatter : AmountChooserMixinBase.FiatFormatter {

    override fun formatFlow(tokenFlow: Flow<Token>, amountFlow: Flow<BigDecimal>): Flow<CharSequence> {
        return combine(tokenFlow, amountFlow) { token, amount ->
            token.amountToFiat(amount).formatAsCurrency(token.currency)
        }
    }
}
