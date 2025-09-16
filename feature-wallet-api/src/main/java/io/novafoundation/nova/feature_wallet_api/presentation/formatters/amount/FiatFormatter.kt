package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrencyNoAbbreviation
import io.novafoundation.nova.feature_currency_api.presentation.formatters.simpleFormatAsCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import java.math.BigDecimal

interface FiatFormatter {

    fun formatFiat(fiatAmount: BigDecimal, currency: Currency, config: FiatConfig = FiatConfig()): CharSequence
}

class RealFiatFormatter(
    private val fractionStylingFormatter: FractionStylingFormatter
) : FiatFormatter {

    override fun formatFiat(fiatAmount: BigDecimal, currency: Currency, config: FiatConfig): CharSequence {
        var formattedFiat = when (config.abbreviationStyle) {
            FiatConfig.AbbreviationStyle.DEFAULT_ABBREVIATION -> fiatAmount.formatAsCurrency(currency, config.roundingMode)
            FiatConfig.AbbreviationStyle.NO_ABBREVIATION -> fiatAmount.formatAsCurrencyNoAbbreviation(currency)
            FiatConfig.AbbreviationStyle.SIMPLE_ABBREVIATION -> fiatAmount.simpleFormatAsCurrency(currency, config.roundingMode)
        }

        if (config.estimatedFiat) {
            formattedFiat = "~$formattedFiat"
        }

        return formattedFiat.applyFractionStyling(fractionStylingFormatter, config.fractionPartStyling)
    }
}
