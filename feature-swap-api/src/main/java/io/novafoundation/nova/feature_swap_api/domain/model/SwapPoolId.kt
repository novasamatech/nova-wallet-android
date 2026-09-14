package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

/**
 * Identifies the on-chain liquidity pool a swap edge trades through.
 *
 * A route must not enter the same pool twice. Hydration router pre-computes every hop of a buy
 * from the state before execution and passes the result as an exact per-hop limit, so a pool
 * modified by an earlier hop of the same route reverts the later hop with a slippage error.
 */
data class SwapPoolId(val chainId: ChainId, val identifier: String)
