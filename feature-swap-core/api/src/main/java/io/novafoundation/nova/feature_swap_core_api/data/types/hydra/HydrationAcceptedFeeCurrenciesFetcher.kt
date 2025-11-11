package io.novafoundation.nova.feature_swap_core_api.data.types.hydra

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

interface HydrationAcceptedFeeCurrenciesFetcher {

    suspend fun fetchAcceptedFeeCurrencies(chain: Chain): Result<Set<ChainAssetId>>
}
