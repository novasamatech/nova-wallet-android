package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface SwapGraphEdge : QuotableEdge {

    suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation

    /**
     * Append current swap edge execution to the existing transaction
     * Return null if it is not possible, indicating that the new transaction should be initiated to handle this edge via
     * [beginOperation]
     */
    suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation?
}

interface QuotableEdge : Edge<FullChainAssetId> {

    suspend fun quote(
        amount: Balance,
        direction: SwapDirection
    ): Balance
}

typealias SwapGraph = Graph<FullChainAssetId, SwapGraphEdge>
typealias SwapPath = Path<SwapGraphEdge>
