package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.alphaSymbol
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.ltrIsolatedRate
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitFailureLimits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.dustRemainder
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitDustRemainderUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitFailureValues
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitFeeShortfallUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorExitRateUpdateUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatPercents
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.title
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Named

class BittensorExitFormatter @Inject constructor(
    private val resources: ResourceManager,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    @Named("BittensorWithdrawalDuration") private val durationFormatter: DurationFormatter
) {

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

    /** The values the exit alerts name, from the snapshot the validation read. */
    fun failureValues(
        payload: BittensorExitValidationPayload,
        position: BittensorExitPosition,
        limits: BittensorExitFailureLimits
    ): BittensorExitFailureValues {
        return BittensorExitFailureValues(
            minimum = tao(position, payload.preflight.minStake),
            maximum = amount(position, limits.maximum, alpha = !position.isRoot).token.toString(),
            priceImpact = payload.quote.priceImpact.formatPercents(),
            nativeSymbol = position.asset.token.configuration.symbol.value,
            holdEta = limits.holdDuration?.let(durationFormatter::format),
            feeShortfall = feeShortfall(payload, position),
            dustRemainder = payload.dustRemainder()?.let { BittensorExitDustRemainderUi(tao(position, it.remainderTao), tao(position, it.minimumTao)) },
            rateUpdate = payload.latestQuote?.let { BittensorExitRateUpdateUi("1 ${alphaSymbol(position)}", rate(position, payload.quote), rate(position, it)) }
        )
    }

    private fun tao(position: BittensorExitPosition, planks: BigInteger): String {
        val token = position.asset.token
        return tokenFormatter.formatToken(token.amountFromPlanks(planks), token.configuration.symbol).toString()
    }

    // A batch keeps the fee and the existential deposit in the free balance; a single root unstake pays the fee from its position.
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

    /** The Swap rate explanation with the quoted Nova commission; a root unstake has none. */
    fun swapRateDescription(position: BittensorExitPosition, quote: BittensorExitQuote): String? {
        if (position.isRoot) return null
        val percentage = quote.serviceFeeRate?.movePointRight(2)?.toStripTrailingZerosString() ?: return null
        return resources.getString(
            R.string.bittensor_swap_rate_sell_description,
            position.asset.token.configuration.symbol.value,
            position.subnet.title(position.netuid.value, resources),
            percentage
        )
    }

    /** Where the network fee of the exit is paid from. */
    fun feeSource(draft: BittensorExitDraft): String = resources.getString(
        if (draft.feeFromStake) R.string.bittensor_exit_fee_from_stake else R.string.bittensor_exit_fee_from_free,
        draft.position.asset.token.configuration.symbol.value
    )

    fun averagePriceDescription(position: BittensorExitPosition): String? = position.subnet?.let {
        resources.getString(R.string.bittensor_average_price_description, alphaSymbol(position), it.title(resources))
    }

    fun exchangeRate(draft: BittensorExitDraft?): String {
        draft ?: return "—"
        return exchangeRate(draft.position, draft.quote)
    }

    fun exchangeRate(position: BittensorExitPosition, quote: BittensorExitQuote): String {
        return ltrIsolatedRate("1 ${alphaSymbol(position)}", rate(position, quote))
    }

    private fun rate(position: BittensorExitPosition, quote: BittensorExitQuote): String {
        val ratio = quote.estimatedTaoOut.toBigDecimal()
            .divide(quote.amount.toBigDecimal(), BittensorProtocolUnits.ALPHA_PRECISION, RoundingMode.DOWN)
        return tokenFormatter.formatToken(ratio, position.asset.token.configuration.symbol).toString()
    }

    private fun alphaSymbol(position: BittensorExitPosition): String = position.subnet.alphaSymbol(position.netuid.value)
}

/** The spot price of the current [quote], otherwise the subnet price; null unless it is positive. */
internal fun bittensorAlphaSpotPrice(position: BittensorExitPosition, quote: BittensorExitQuote?): BigInteger? {
    return (quote?.spotPrice ?: position.subnet?.priceInRao)?.takeIf { it.isPositive() }
}
