package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

data class SwapRouteEdgeId(
    val from: FullChainAssetId,
    val to: FullChainAssetId,
    val exchange: String,
    val pool: String?,
)

fun SwapGraphEdge.routeId() = SwapRouteEdgeId(from, to, exchangeId, poolId?.identifier)
