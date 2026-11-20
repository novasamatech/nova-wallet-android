package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.findAllPossibleDestinations
import io.novafoundation.nova.common.utils.graph.vertices
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun <N, E : Edge<N>> Graph<N, E>.assetsForSelection(
    opposite: N,
    selectingPay: Boolean,
    filter: EdgeVisitFilter<E>
): Set<N> {
    if (!selectingPay) return findAllPossibleDestinations(opposite, filter)

    // Routes are directed. Reversing edges would also reverse predecessor-dependent fee checks.
    return vertices().filterTo(mutableSetOf()) { candidate ->
        currentCoroutineContext().ensureActive()
        candidate != opposite && opposite in findAllPossibleDestinations(candidate, filter)
    }
}
