package io.novafoundation.nova.feature_swap_core_api.data.paths

import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.NodeVisitFilter
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.BestPathQuote
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import java.math.BigInteger

interface PathQuoter<E : QuotableEdge> {

    interface Factory {

        fun <E : QuotableEdge> create(
            graph: Graph<FullChainAssetId, E>,
            computationalScope: CoroutineScope,
            filter: NodeVisitFilter<FullChainAssetId>? = null
        ): PathQuoter<E>
    }

    suspend fun findBestPath(
        chainAssetIn: Chain.Asset,
        chainAssetOut: Chain.Asset,
        amount: BigInteger,
        swapDirection: SwapDirection,
    ): BestPathQuote<E>
}
