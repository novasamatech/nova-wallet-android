package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class SwapQuoteArgs(
    val tokenIn: Token,
    val tokenOut: Token,
    val amount: Balance,
    val swapDirection: SwapDirection,
)

open class SwapFeeArgs(
    val assetIn: Chain.Asset,
    val slippage: Fraction,
    val executionPath: Path<SegmentExecuteArgs>,
    val direction: SwapDirection,
    val firstSegmentFees: FeePaymentCurrency
)

class SegmentExecuteArgs(
    val quotedSwapEdge: QuotedEdge<SwapGraphEdge>,
)

