package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.dustRemainder
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitDustRemainderUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitFailureValues
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitFeeShortfallUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitRateUpdateUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class BittensorExitFormatter(
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter
) {

    private companion object {

        const val IMPACT_SCALE = 6
    }

    /** Alpha is valued at [quote]'s spot price when there is one, as [bittensorAlphaSpotPrice]. */
    fun amount(position: BittensorExitPosition, planks: BigInteger, alpha: Boolean, quote: BittensorExitQuote? = null): AmountModel {
        val token = position.asset.token
        val value = if (alpha) BigDecimal(planks, position.amountPrecision) else token.amountFromPlanks(planks)
        val tao = if (alpha) bittensorAlphaSpotPrice(position, quote)?.let { value * token.amountFromPlanks(it) } else value
        val fiat = tao?.let(token::toFiatOrNull)
        val symbol = if (alpha) TokenSymbol(alphaSymbol(position)) else token.configuration.symbol
        return AmountModel(
            token = tokenFormatter.formatToken(value, symbol),
            fiat = fiat?.let { fiatFormatter.formatFiat(it, token.currency) }
        )
    }

    /** The values the iOS exit alerts name, from the snapshot the validation read; [maximum] is in the position's units. */
    fun failureValues(
        payload: BittensorExitValidationPayload,
        position: BittensorExitPosition,
        maximum: BigInteger,
        holdEta: String?
    ): BittensorExitFailureValues {
        return BittensorExitFailureValues(
            minimum = tao(position, payload.preflight.minStake),
            maximum = amount(position, maximum, alpha = !position.isRoot).token.toString(),
            priceImpact = priceImpact(payload.quote),
            holdEta = holdEta,
            feeShortfall = feeShortfall(payload, position),
            dustRemainder = payload.dustRemainder()?.let { BittensorExitDustRemainderUi(tao(position, it.remainderTao), tao(position, it.minimumTao)) },
            rateUpdate = payload.latestQuote?.let { BittensorExitRateUpdateUi("1 ${alphaSymbol(position)}", rate(position, payload.quote), rate(position, it)) }
        )
    }

    private fun tao(position: BittensorExitPosition, planks: BigInteger): String {
        val token = position.asset.token
        return tokenFormatter.formatToken(token.amountFromPlanks(planks), token.configuration.symbol).toString()
    }

    // iOS: a batch keeps the fee and the existential deposit in the free balance; a single root unstake pays the fee from its position.
    private fun feeShortfall(payload: BittensorExitValidationPayload, position: BittensorExitPosition): BittensorExitFeeShortfallUi {
        val quote = payload.quote
        if (!position.isRoot || quote.plan?.isBatched == true || quote.exitHotkeys.size > 1) {
            val required = payload.fee + payload.existentialDeposit
            val missing = (required - payload.transferable).max(BigInteger.ZERO)
            return BittensorExitFeeShortfallUi(tao(position, required), tao(position, payload.transferable), tao(position, missing))
        }
        val stake = quote.plan?.slices?.singleOrNull()?.let { payload.preflight.groupMembers[it.hotkey]?.stake } ?: payload.preflight.stake
        return BittensorExitFeeShortfallUi(tao(position, payload.fee), tao(position, stake), missing = null)
    }

    // iOS `NumberFormatter.percentSingle`: the pool price move against spot, at most two fraction digits rounded down.
    private fun priceImpact(quote: BittensorExitQuote?): String {
        val spot = quote?.spotPrice?.takeIf { it.isPositive() }
        val executed = quote?.postTradePrice
        val impact = if (spot != null && executed != null) {
            (spot - executed).abs().toBigDecimal().divide(spot.toBigDecimal(), IMPACT_SCALE, RoundingMode.DOWN)
        } else {
            BigDecimal.ZERO
        }
        return impact.movePointRight(2).formatAsPercentage()
    }

    fun exchangeRate(draft: BittensorExitDraft?): String {
        draft ?: return "—"
        return exchangeRate(draft.position, draft.quote)
    }

    fun exchangeRate(position: BittensorExitPosition, quote: BittensorExitQuote): String {
        return "1 ${alphaSymbol(position)} ≈ ${rate(position, quote)}"
    }

    private fun rate(position: BittensorExitPosition, quote: BittensorExitQuote): String {
        val ratio = quote.estimatedTaoOut.toBigDecimal()
            .divide(quote.amount.toBigDecimal(), BittensorProtocolUnits.ALPHA_PRECISION, RoundingMode.DOWN)
        return tokenFormatter.formatToken(ratio, position.asset.token.configuration.symbol).toString()
    }

    private fun alphaSymbol(position: BittensorExitPosition): String = bittensorAlphaSymbol(position.netuid.value, position.subnet?.symbol)
}

/** iOS `alphaSpotPrice`: the spot price of the current [quote], otherwise the subnet price; null unless it is positive. */
internal fun bittensorAlphaSpotPrice(position: BittensorExitPosition, quote: BittensorExitQuote?): BigInteger? {
    return (quote?.spotPrice ?: position.subnet?.priceInRao)?.takeIf { it.isPositive() }
}
