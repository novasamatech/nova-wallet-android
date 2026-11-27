package io.novafoundation.nova.feature_swap_impl.domain.exchanger

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlin.time.Duration

data class ExchangerAQuoteArgs(
    val assetIn: Chain.Asset,
    val assetOut: Chain.Asset,
    val amount: Balance,
    val swapDirection: SwapDirection
)

sealed class ExchangerAQuoteResult {

    abstract val args: ExchangerAQuoteArgs

    abstract val limits: ExchangerAPayLimits

    class Quote(
        override val args: ExchangerAQuoteArgs,
        override val limits: ExchangerAPayLimits,
        val amountIn: ChainAssetWithAmount,
        val amountOut: ChainAssetWithAmount,
        // Provider fees are already included in amountOut. The wallet's deposit transaction fee is separate.
        val depositFee: ChainAssetWithAmount?,
        val withdrawalFee: ChainAssetWithAmount?,
        val executionTime: ClosedRange<Duration>?,
        val warning: String?
    ) : ExchangerAQuoteResult()

    class OutOfRange(
        override val args: ExchangerAQuoteArgs,
        override val limits: ExchangerAPayLimits,
        val reason: Reason
    ) : ExchangerAQuoteResult() {

        enum class Reason { BELOW_MINIMUM, ABOVE_MAXIMUM }
    }
}

/** Both limits are in the pay asset's smallest units. Null maximum means unbounded. */
class ExchangerAPayLimits(
    val min: Balance,
    val max: Balance?
)
