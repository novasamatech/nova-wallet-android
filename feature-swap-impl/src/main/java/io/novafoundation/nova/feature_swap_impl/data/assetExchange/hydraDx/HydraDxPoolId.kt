package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.feature_swap_api.domain.model.SwapPoolId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

object HydraDxPoolId {

    const val XYK = "xyk"
    const val AAVE = "aave"

    fun omnipool(chainId: ChainId): SwapPoolId {
        return SwapPoolId(chainId, "omnipool")
    }

    fun stableswap(chainId: ChainId, poolId: HydraDxAssetId): SwapPoolId {
        return SwapPoolId(chainId, "stableswap:$poolId")
    }

    /**
     * Identifies a pool that holds exactly one asset pair, so both trade directions map to the same pool
     */
    fun pair(poolType: String, from: FullChainAssetId, to: FullChainAssetId): SwapPoolId {
        val pairIdentifier = listOf(from.assetId, to.assetId).sorted().joinToString(separator = "-")

        return SwapPoolId(from.chainId, "$poolType:$pairIdentifier")
    }
}
