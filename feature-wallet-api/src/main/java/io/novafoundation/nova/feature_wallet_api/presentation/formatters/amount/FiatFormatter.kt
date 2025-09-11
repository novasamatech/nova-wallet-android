package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrencyNoAbbreviation
import io.novafoundation.nova.feature_currency_api.presentation.formatters.simpleFormatAsCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import java.math.BigDecimal
import java.math.RoundingMode

interface FiatFormatter : GenericFiatFormatter<CharSequence>

class RealFiatFormatter : FiatFormatter {

    override fun formatFiatNoAbbreviation(amount: BigDecimal, currency: Currency, config: FiatConfig): CharSequence {
        return amount.formatAsCurrencyNoAbbreviation(currency)
    }

    override fun formatAsCurrency(amount: BigDecimal, currency: Currency, roundingMode: RoundingMode, config: FiatConfig): CharSequence {
        return amount.formatAsCurrency(currency, roundingMode)
    }

    override fun simpleFormatAsCurrency(amount: BigDecimal, currency: Currency, roundingMode: RoundingMode, config: FiatConfig): CharSequence {
        return amount.simpleFormatAsCurrency(currency, roundingMode)
    }
}
