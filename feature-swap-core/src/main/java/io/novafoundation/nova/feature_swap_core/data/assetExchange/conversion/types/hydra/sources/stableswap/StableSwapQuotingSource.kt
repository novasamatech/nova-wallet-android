package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface StableSwapQuotingSource : HydraDxQuotingSource<StableSwapQuotingSource.Edge> {

    val chain: Chain

    interface Edge : QuotableEdge {

        val fromAsset: RemoteAndLocalId

        val toAsset: RemoteAndLocalId

        val poolId: HydraDxAssetId
    }
}
