package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource

interface AavePoolQuotingSource : HydraDxQuotingSource<AavePoolQuotingSource.Edge> {

    interface Edge : QuotableEdge {

        val fromAsset: RemoteAndLocalId

        val toAsset: RemoteAndLocalId
    }
}
