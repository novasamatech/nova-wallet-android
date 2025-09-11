package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import androidx.core.text.buildSpannedString
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatWithFullAmount
import io.novafoundation.nova.common.utils.withTokenSymbol
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrencyNoAbbreviation
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FractionStylingFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionStylingSize
import java.math.BigDecimal

interface AmountFormatter : GenericAmountFormatter<AmountModel>

class RealAmountFormatter(
    private val fractionStylingFormatter: FractionStylingFormatter
) : AmountFormatter {

    override fun formatAmountToAmountModel(
        amount: BigDecimal,
        token: TokenBase,
        config: AmountConfig
    ): AmountModel {
        return AmountModel(
            token = buildSpannedString {
                append(config.tokenAmountSign.signSymbol)
                append(formatAmount(config, amount, token))
            },
            fiat = formatFiat(amount, token, config)
        )
    }

    private fun formatAmount(
        config: AmountConfig,
        amount: BigDecimal,
        token: TokenBase
    ): CharSequence {
        val unsignedTokenAmount = if (config.useAbbreviation) {
            if (config.includeAssetTicker) {
                amount.formatTokenAmount(token.configuration, config.roundingMode)
            } else {
                amount.format(config.roundingMode)
            }
        } else {
            val unformattedAmount = amount.formatWithFullAmount()

            if (config.includeAssetTicker) {
                unformattedAmount.withTokenSymbol(token.configuration.symbol)
            } else {
                unformattedAmount
            }
        }

        return unsignedTokenAmount.applyFractionStyling(config.tokenFractionStylingSize)
    }

    private fun formatFiat(
        amount: BigDecimal,
        token: TokenBase,
        config: AmountConfig
    ): String? {
        val fiatAmount = token.amountToFiat(amount)
            .takeIf { it != BigDecimal.ZERO || config.includeZeroFiat }

        var formattedFiat = if (config.useAbbreviation) {
            fiatAmount?.formatAsCurrency(token.currency, config.roundingMode)
        } else {
            fiatAmount?.formatAsCurrencyNoAbbreviation(token.currency)
        }

        if (config.estimatedFiat && formattedFiat != null) {
            formattedFiat = "~$formattedFiat"
        }

        return formattedFiat
    }

    private fun CharSequence.applyFractionStyling(fractionStylingSize: FractionStylingSize): CharSequence {
        return when (fractionStylingSize) {
            FractionStylingSize.Default -> this
            is FractionStylingSize.AbsoluteSize -> fractionStylingFormatter.formatFraction(this, fractionStylingSize.sizeRes)
        }
    }
}
