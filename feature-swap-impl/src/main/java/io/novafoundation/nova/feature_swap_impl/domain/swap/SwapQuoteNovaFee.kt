package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.HydraDxQuotableEdge

/**
 * Checks whether the quoted path includes any Hydra swap leg.
 *
 * Edge-type detection is required: chain-id detection over-matches cross-chain
 * transfer edges to/from Hydra, which are not swaps.
 */
fun SwapQuote.involvesHydraSwap(): Boolean {
    return quotedPath.path.any { it.edge is HydraDxQuotableEdge }
}
