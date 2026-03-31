package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.rateAgainst
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import java.math.BigDecimal

/**
 * Checks whether any segment in the quoted path involves a swap on a Hydration chain.
 */
fun SwapQuote.involvesHydraSwap(chainsById: ChainsById): Boolean {
    return quotedPath.path.any { quotedEdge ->
        val chainId = quotedEdge.edge.from.chainId
        val chain = chainsById[chainId] ?: return@any false
        chain.swap.hydraDxSupported()
    }
}

/**
 * Returns the display amount out - after subtracting the Nova commission fee for Hydra swaps.
 * For non-Hydra swaps, returns the original amount out.
 */
fun SwapQuote.displayAmountOut(chainsById: ChainsById): Balance {
    return if (involvesHydraSwap(chainsById)) {
        NovaSwapCommission.amountOutAfterFee(planksOut)
    } else {
        planksOut
    }
}

/**
 * Computes the swap rate using the display (post-fee) amount out.
 */
fun SwapQuote.displaySwapRate(chainsById: ChainsById): BigDecimal {
    val displayOut = displayAmountOut(chainsById)
    val displayAmountOut = ChainAssetWithAmount(assetOut, displayOut)
    return amountIn rateAgainst displayAmountOut
}
