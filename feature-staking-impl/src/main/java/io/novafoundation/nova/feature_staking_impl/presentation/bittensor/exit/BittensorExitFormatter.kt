package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class BittensorExitFormatter(
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter
) {

    fun amount(position: BittensorExitPosition, planks: BigInteger, alpha: Boolean): AmountModel {
        val token = position.asset.token
        val value = if (alpha) BigDecimal(planks, position.amountPrecision) else token.amountFromPlanks(planks)
        val tao = if (alpha) position.subnet?.priceInRao?.let { value * token.amountFromPlanks(it) } else value
        val fiat = tao?.let(token::toFiatOrNull)
        val symbol = if (alpha) TokenSymbol(position.subnet?.symbol?.ifBlank { "—" } ?: "—") else token.configuration.symbol
        return AmountModel(
            token = tokenFormatter.formatToken(value, symbol),
            fiat = fiat?.let { fiatFormatter.formatFiat(it, token.currency) }
        )
    }

    fun exchangeRate(draft: BittensorExitDraft?): String {
        draft ?: return "—"
        return exchangeRate(draft.position, draft.quote)
    }

    fun exchangeRate(position: BittensorExitPosition, quote: BittensorExitQuote): String {
        val ratio = quote.estimatedTaoOut.toBigDecimal()
            .divide(quote.amount.toBigDecimal(), BittensorProtocolUnits.ALPHA_PRECISION, RoundingMode.DOWN)
        val nativeAmount = tokenFormatter.formatToken(ratio, position.asset.token.configuration.symbol)
        val alphaSymbol = position.subnet?.symbol?.ifBlank { "—" } ?: "—"
        return "1 $alphaSymbol ≈ $nativeAmount"
    }
}
