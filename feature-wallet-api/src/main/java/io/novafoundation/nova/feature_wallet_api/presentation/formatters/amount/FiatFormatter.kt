package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrencyNoAbbreviation
import io.novafoundation.nova.feature_currency_api.presentation.formatters.simpleFormatAsCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import java.math.BigDecimal

interface FiatFormatter : GenericFiatFormatter<CharSequence>

class RealFiatFormatter(
    private val fractionStylingFormatter: FractionStylingFormatter
) : FiatFormatter {

    override fun formatFiat(fiatAmount: BigDecimal, currency: Currency, config: FiatConfig): CharSequence {
        var formattedFiat = when (config.style) {
            FiatConfig.Style.DEFAULT -> fiatAmount.formatAsCurrency(currency, config.roundingMode)
            FiatConfig.Style.NO_ABBREVIATION -> fiatAmount.formatAsCurrencyNoAbbreviation(currency)
            FiatConfig.Style.SIMPLE -> fiatAmount.simpleFormatAsCurrency(currency, config.roundingMode)
        }

        if (config.estimatedFiat) {
            formattedFiat = "~$formattedFiat"
        }

        return formattedFiat.applyFractionStyling(fractionStylingFormatter, config.fractionStylingSize)
    }
}
