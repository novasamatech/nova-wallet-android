package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import androidx.core.text.buildSpannedString
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatWithFullAmount
import io.novafoundation.nova.common.utils.withTokenSymbol
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import java.math.BigDecimal

interface TokenFormatter : GenericTokenFormatter<CharSequence>

class RealTokenFormatter(
    private val fractionStylingFormatter: FractionStylingFormatter
) : TokenFormatter {

    override fun formatToken(
        amount: BigDecimal,
        token: TokenBase,
        config: TokenConfig
    ): CharSequence {
        return buildSpannedString {
            append(config.tokenAmountSign.signSymbol)
            append(formatAmount(amount, token, config))
        }
    }

    private fun formatAmount(
        amount: BigDecimal,
        token: TokenBase,
        config: TokenConfig
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

        return unsignedTokenAmount.applyFractionStyling(fractionStylingFormatter, config.fractionStylingSize)
    }
}
