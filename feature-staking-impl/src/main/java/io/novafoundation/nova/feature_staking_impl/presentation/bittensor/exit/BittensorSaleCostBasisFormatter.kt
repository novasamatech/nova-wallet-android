package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.alphaSymbol
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.isNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedPurchaseTotals
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import javax.inject.Inject
import java.math.BigDecimal
import java.math.BigInteger

data class BittensorSaleProfitUi(val amount: AmountModel, @ColorRes val colorRes: Int)

data class BittensorAveragePriceUi(val amount: AmountModel, @ColorRes val colorRes: Int)

class BittensorSaleCostBasisFormatter @Inject constructor(
    private val resources: ResourceManager,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter
) {

    fun averagePrice(basis: BittensorEstimatedCostBasis?, position: BittensorExitPosition?, netuid: Int): AmountModel {
        return averagePrice(basis, netuid, position?.asset?.token, position?.subnet?.symbol)
    }

    fun averagePrice(basis: BittensorEstimatedCostBasis?, netuid: Int, token: TokenBase?, alphaSymbol: String?): AmountModel {
        val totals = basis?.forSubnet(netuid)
        if (totals == null) {
            return if (basis?.canEstimate(netuid) == true) {
                AmountModel(resources.getString(R.string.common_not_available), resources.getString(R.string.bittensor_no_purchases))
            } else {
                AmountModel("—", null)
            }
        }
        if (token == null) return AmountModel("—", null)
        return averagePriceOf(totals, token, alphaSymbol)
    }

    fun averagePrice(sale: BittensorSaleCostBasis, position: BittensorExitPosition?): AmountModel {
        return averagePrice(sale, position?.asset?.token, position?.let { it.subnet.alphaSymbol(it.netuid.value) })
    }

    fun averagePrice(sale: BittensorSaleCostBasis, token: TokenBase?, alphaSymbol: String?): AmountModel = when (sale) {
        is BittensorSaleCostBasis.Known -> token?.let { averagePriceOf(sale.totals, it, alphaSymbol) } ?: AmountModel("—", null)
        BittensorSaleCostBasis.NoPurchases -> AmountModel(
            resources.getString(R.string.common_not_available),
            resources.getString(R.string.bittensor_no_purchases)
        )
        BittensorSaleCostBasis.Unknown -> AmountModel("—", null)
    }

    /** Earned needs settled totals: a still-settling history may miss the latest purchase. */
    fun profit(sale: BittensorSaleCostBasis?, quote: BittensorExitQuote?, token: TokenBase?): BittensorSaleProfitUi {
        val totals = (sale as? BittensorSaleCostBasis.Known)?.takeIf { it.settled }?.totals
        if (totals == null || quote == null) return profit(null, null)
        return profit(token?.amountFromPlanks(estimatedSaleProfit(totals, quote)), token)
    }

    private fun averagePriceOf(totals: BittensorEstimatedPurchaseTotals, token: TokenBase, alphaSymbol: String?) = AmountModel(
        tokenFormatter.formatToken(totals.averagePrice, token.configuration.symbol),
        alphaSymbol?.takeIf { it.isNotBlank() }
            ?.let { resources.getString(R.string.bittensor_avg_buy_price_per, it) }
    )

    fun projectedAveragePrice(
        basis: BittensorEstimatedCostBasis?,
        quote: BittensorSubnetBuyQuote?,
        netuid: Int,
        token: TokenBase,
        alphaSymbol: String?
    ): BittensorAveragePriceUi {
        val current = basis?.forSubnet(netuid)
        if (current == null || quote == null) {
            return BittensorAveragePriceUi(averagePrice(basis, netuid, token, alphaSymbol), R.color.text_primary)
        }
        val projected = current.adding(quote.totalTaoInPlanks, quote.alphaOutInPlanks)
        val difference = projected.averagePrice.compareTo(current.averagePrice)
        val prefix = when {
            difference > 0 -> "▲ "
            difference < 0 -> "▼ "
            else -> ""
        }
        val color = when {
            difference > 0 -> R.color.text_negative
            difference < 0 -> R.color.text_positive
            else -> R.color.text_primary
        }
        val previous = tokenFormatter.formatToken(current.averagePrice, token.configuration.symbol, TokenConfig(includeAssetTicker = false))
        val detail = alphaSymbol?.takeIf { it.isNotBlank() }?.let { resources.getString(R.string.bittensor_avg_buy_price_from, previous.toString().trim(), it) }
        return BittensorAveragePriceUi(
            AmountModel(prefix + tokenFormatter.formatToken(projected.averagePrice, token.configuration.symbol), detail),
            color
        )
    }

    /** "You earned" on a completed sale; null without settled purchase totals, so the row stays hidden (iOS). */
    fun completedProfit(
        sale: BittensorSaleCostBasis?,
        quote: BittensorExitQuote,
        outcome: BittensorOperationOutcome?,
        token: TokenBase
    ): BittensorSaleProfitUi? {
        val totals = (sale as? BittensorSaleCostBasis.Known)?.takeIf { it.settled }?.totals ?: return null
        return profit(token.amountFromPlanks(completedSaleProfit(totals, quote, outcome)), token)
    }

    fun profit(basis: BittensorEstimatedCostBasis?, draft: BittensorExitDraft?, netuid: Int): BittensorSaleProfitUi {
        val totals = basis?.forSubnet(netuid)
        if (totals == null || draft == null) return BittensorSaleProfitUi(AmountModel("—", null), R.color.text_secondary)

        val token = draft.position.asset.token
        val amount = token.amountFromPlanks(estimatedSaleProfit(totals, draft.quote))
        return profit(amount, token)
    }

    fun profit(amount: BigDecimal?, token: TokenBase?): BittensorSaleProfitUi {
        if (amount == null || token == null) return BittensorSaleProfitUi(AmountModel("—", null), R.color.text_secondary)
        val sign = when {
            amount.isPositive -> "+"
            amount.isNegative -> "−"
            else -> ""
        }
        val fiat = token.toFiatOrNull(amount.abs())?.let { fiatFormatter.formatFiat(it, token.currency) }
        val detail = fiat?.let { "≈ " + (if (amount.isNegative) "−" else "") + it }
        val color = when {
            amount.isPositive -> R.color.text_positive
            amount.isNegative -> R.color.text_negative
            else -> R.color.text_primary
        }
        return BittensorSaleProfitUi(
            AmountModel(sign + tokenFormatter.formatToken(amount.abs(), token.configuration.symbol), detail),
            color
        )
    }
}

internal fun estimatedSaleProfit(totals: BittensorEstimatedPurchaseTotals, quote: BittensorExitQuote) =
    totals.estimatedProfit(quote.amount, quote.estimatedTaoOut - quote.serviceFee)

/** The executed sale when the chain reported it (alpha sold, TAO out before the network fee), the reviewed quote otherwise. */
internal fun completedSaleProfit(totals: BittensorEstimatedPurchaseTotals, quote: BittensorExitQuote, outcome: BittensorOperationOutcome?): BigInteger {
    val soldAlpha = outcome?.alpha
    val executedTao = outcome?.tao
    val paidService = if (quote.serviceFee.signum() == 0) BigInteger.ZERO else outcome?.serviceFee
    if (soldAlpha == null || executedTao == null || paidService == null) return estimatedSaleProfit(totals, quote)
    return totals.estimatedProfit(soldAlpha, executedTao - paidService)
}
