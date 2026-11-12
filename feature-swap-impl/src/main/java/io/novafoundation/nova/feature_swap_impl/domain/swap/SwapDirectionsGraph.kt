package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraph
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun combineSwapDirections(directionsByExchange: List<Flow<List<SwapGraphEdge>>>): Flow<SwapGraph> {
    if (directionsByExchange.isEmpty()) return flowOf(Graph.create(emptyList<SwapGraphEdge>()))

    // An incomplete graph cannot establish that a pair has no route.
    return directionsByExchange.combine()
        .map { connections -> Graph.create(connections.flatten()) }
}
