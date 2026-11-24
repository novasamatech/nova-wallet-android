package io.novafoundation.nova.feature_swap_impl.domain.exchanger

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import kotlin.time.Duration

sealed class ExchangerAQuoteResult {

    abstract val args: SwapQuoteArgs

    abstract val limits: ExchangerAPayLimits

    class Quote(
        override val args: SwapQuoteArgs,
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
        override val args: SwapQuoteArgs,
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
