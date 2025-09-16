package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import androidx.core.text.buildSpannedString
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatWithFullAmount
import io.novafoundation.nova.common.utils.withTokenSymbol
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import java.math.BigDecimal
import java.math.BigInteger

interface TokenFormatter {

    fun formatToken(
        amount: BigDecimal,
        token: TokenSymbol,
        config: TokenConfig = TokenConfig()
    ): CharSequence
}

class RealTokenFormatter(
    private val fractionStylingFormatter: FractionStylingFormatter
) : TokenFormatter {

    override fun formatToken(
        amount: BigDecimal,
        token: TokenSymbol,
        config: TokenConfig
    ): CharSequence {
        return buildSpannedString {
            append(config.tokenAmountSign.signSymbol)
            append(formatAmount(amount, token, config))
        }
    }

    private fun formatAmount(
        amount: BigDecimal,
        token: TokenSymbol,
        config: TokenConfig
    ): CharSequence {
        val unsignedTokenAmount = if (config.useAbbreviation) {
            if (config.includeAssetTicker) {
                amount.formatTokenAmount(token, config.roundingMode)
            } else {
                amount.format(config.roundingMode)
            }
        } else {
            val unformattedAmount = amount.formatWithFullAmount()

            if (config.includeAssetTicker) {
                unformattedAmount.withTokenSymbol(token)
            } else {
                unformattedAmount
            }
        }

        return unsignedTokenAmount.applyFractionStyling(fractionStylingFormatter, config.fractionPartStyling)
    }
}

fun TokenFormatter.formatToken(amountInPlanks: BigInteger, asset: Asset): CharSequence {
    return formatToken(asset.token.amountFromPlanks(amountInPlanks), asset.token.configuration.symbol)
}
