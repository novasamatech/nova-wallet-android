package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novasama.substrate_sdk_android.runtime.AccountId

interface XYKSwapQuotingSource : HydraDxQuotingSource<XYKSwapQuotingSource.Edge> {

    interface Edge : QuotableEdge {

        val fromAsset: RemoteAndLocalId

        val toAsset: RemoteAndLocalId

        val poolAddress: AccountId
    }
}
