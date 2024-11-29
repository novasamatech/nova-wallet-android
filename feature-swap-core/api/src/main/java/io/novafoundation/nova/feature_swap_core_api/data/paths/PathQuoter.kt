package io.novafoundation.nova.feature_swap_core_api.data.paths

import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.BestPathQuote
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface PathQuoter<E : QuotableEdge> {

    interface Factory {

        fun <E : QuotableEdge> create(
            graphFlow: Flow<Graph<FullChainAssetId, E>>,
            computationalScope: CoroutineScope,
            pathFeeEstimation: PathFeeEstimator<E>? = null,
            filter: EdgeVisitFilter<E>? = null
        ): PathQuoter<E>
    }

    suspend fun findBestPath(
        chainAssetIn: Chain.Asset,
        chainAssetOut: Chain.Asset,
        amount: BigInteger,
        swapDirection: SwapDirection,
    ): BestPathQuote<E>
}
