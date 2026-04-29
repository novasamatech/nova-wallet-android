package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.rateAgainst
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.HydraDxQuotableEdge
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import java.math.BigDecimal

/**
 * Checks whether the quoted path includes any Hydra swap leg.
 *
 * Edge-type detection is required: chain-id detection over-matches cross-chain
 * transfer edges to/from Hydra, which are not swaps.
 */
fun SwapQuote.involvesHydraSwap(): Boolean {
    return quotedPath.path.any { it.edge is HydraDxQuotableEdge }
}

/**
 * Returns the display amount out - after subtracting the Nova commission fee for Hydra swaps.
 * For non-Hydra swaps, returns the original amount out.
 */
fun SwapQuote.displayAmountOut(): Balance {
    return if (involvesHydraSwap()) {
        NovaSwapCommission.amountOutAfterFee(planksOut)
    } else {
        planksOut
    }
}

/**
 * Computes the swap rate using the display (post-fee) amount out.
 */
fun SwapQuote.displaySwapRate(): BigDecimal {
    val displayOut = displayAmountOut()
    val displayAmountOut = ChainAssetWithAmount(assetOut, displayOut)
    return amountIn rateAgainst displayAmountOut
}
