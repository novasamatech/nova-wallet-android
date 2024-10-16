package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface SwapGraphEdge : QuotableEdge {

    suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation

    /**
     * Append current swap edge execution to the existing transaction
     * Return null if it is not possible, indicating that the new transaction should be initiated to handle this edge via
     * [beginOperation]
     */
    suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation?

    suspend fun debugLabel(): String

    /**
     * Whether this Edge fee check should be skipped when adding to after a specified [predecessor]
     * Note that returning true here means that [canPayNonNativeFeesInIntermediatePosition] wont be called and checked
     *
     */
    suspend fun shouldIgnoreFeeRequirementAfter(predecessor: SwapGraphEdge): Boolean

    /**
     * Can be used to define additional restrictions on top of default one, "is able to pay submission fee on origin"
     * This will only be called for intermediate hops for non-utility assets since other cases are always payable
     */
    suspend fun canPayNonNativeFeesInIntermediatePosition(): Boolean
}



typealias SwapGraph = Graph<FullChainAssetId, SwapGraphEdge>
