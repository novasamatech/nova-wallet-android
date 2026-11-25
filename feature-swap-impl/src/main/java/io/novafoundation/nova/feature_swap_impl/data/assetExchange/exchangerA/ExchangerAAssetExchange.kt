package io.novafoundation.nova.feature_swap_impl.data.assetExchange.exchangerA

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.catalogue.ExchangerASwapPair
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.catalogue.ExchangerATaoCatalogue
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotedAmounts
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAQuoteRepository
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountConstraints
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountPrecisionExceeded
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteArgs
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteResult
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.feature_swap_impl.domain.swap.QuoteProviderException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

class ExchangerAAssetExchange @Inject constructor(
    private val catalogue: ExchangerATaoCatalogue,
    private val quotes: ExchangerAQuoteRepository,
    private val constraints: ExchangerAAmountConstraints
) : AssetExchange.External {

    override fun supportsDestination(asset: FullChainAssetId) = asset == ExchangerATaoCatalogue.receiveAsset

    override suspend fun sync() = Unit

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> =
        request { catalogue.availablePairs() }.map { ExchangerAEdge(it) }

    override fun feePaymentOverrides(): List<FeePaymentProviderOverride> = emptyList()

    override fun runSubscriptions(metaAccount: MetaAccount) = emptyFlow<ReQuoteTrigger>()

    private suspend fun <T> request(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (precision: ExchangerAAmountPrecisionExceeded) {
        throw QuoteProviderException.AmountPrecisionExceeded(precision.constraints)
    } catch (error: Exception) {
        throw QuoteProviderException.Unavailable(error)
    }

    private inner class ExchangerAEdge(private val pair: ExchangerASwapPair) : SwapGraphEdge {

        override val exchangeId = "exchanger-a"
        override val from = pair.assetIn.fullId
        override val to = pair.assetOut.fullId
        override val isTerminal = true

        override suspend fun debugLabel() = "Exchanger A"

        override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>) = QuotableEdge.DEFAULT_SEGMENT_WEIGHT

        override fun predecessorHandlesFees(predecessor: SwapGraphEdge) = false

        override suspend fun canTransferOutWholeAccountBalance() = true

        override suspend fun canPayNonNativeFeesInIntermediatePosition() = true

        private fun intermediateAmount(amount: Balance, direction: SwapDirection): Balance {
            val asset = if (direction == SwapDirection.SPECIFIED_IN) pair.assetIn else pair.assetOut
            val unit = Balance.TEN.pow(asset.precision.value - constraints.forAsset(asset).maxFractionDigits)
            val remainder = amount.mod(unit)
            if (remainder == Balance.ZERO) return amount
            return if (direction == SwapDirection.SPECIFIED_IN) amount - remainder else amount + unit - remainder
        }

        override suspend fun quote(amount: Balance, direction: SwapDirection): Balance = quoteAmounts(amount, direction).counterpart

        override suspend fun quoteAmounts(amount: Balance, direction: SwapDirection, isIntermediate: Boolean): QuotedAmounts {
            val requestAmount = if (isIntermediate) intermediateAmount(amount, direction) else amount
            val result = request { quotes.quote(ExchangerAQuoteArgs(pair.assetIn, pair.assetOut, requestAmount, direction)) }
            return when (result) {
                is ExchangerAQuoteResult.OutOfRange -> throw when (result.reason) {
                    ExchangerAQuoteResult.OutOfRange.Reason.BELOW_MINIMUM -> QuoteProviderException.DepositLimit.BelowMinimum(pair.assetIn, result.limits.min)
                    ExchangerAQuoteResult.OutOfRange.Reason.ABOVE_MAXIMUM -> QuoteProviderException.DepositLimit.AboveMaximum(pair.assetIn, result.limits.max)
                }
                is ExchangerAQuoteResult.Quote -> when (direction) {
                    SwapDirection.SPECIFIED_IN -> QuotedAmounts(result.amountIn.amount, result.amountOut.amount)
                    SwapDirection.SPECIFIED_OUT -> QuotedAmounts(result.amountOut.amount, result.amountIn.amount)
                }
            }
        }
    }
}
