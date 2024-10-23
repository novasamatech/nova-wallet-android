package io.novafoundation.nova.feature_swap_core_api.data.paths

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge

interface PathFeeEstimator<E> {

    suspend fun roughlyEstimateFee(path: Path<QuotedEdge<E>>): PathRoughFeeEstimation
}

