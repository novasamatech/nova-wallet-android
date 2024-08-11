package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface AssetConversion<T : Edge<FullChainAssetId>> {

    interface Factory<T : Edge<FullChainAssetId>> {
        fun create(chain: Chain): AssetConversion<T>
    }

    suspend fun availableSwapDirections(): Graph<FullChainAssetId, T>

    suspend fun getPaths(graph: Graph<FullChainAssetId, T>, args: AssetExchangeQuoteArgs): List<Path<T>>

    suspend fun quote(paths: List<Path<T>>, args: AssetExchangeQuoteArgs): AssetExchangeQuote

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>

}
