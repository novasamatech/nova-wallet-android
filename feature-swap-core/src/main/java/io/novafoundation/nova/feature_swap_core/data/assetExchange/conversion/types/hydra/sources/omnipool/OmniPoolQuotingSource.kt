package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteIdAndLocalAsset
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource

interface OmniPoolQuotingSource : HydraDxQuotingSource<OmniPoolQuotingSource.Edge> {

    interface Edge : QuotableEdge {

        val fromAsset: RemoteIdAndLocalAsset

        val toAsset: RemoteIdAndLocalAsset
    }
}
