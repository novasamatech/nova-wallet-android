package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_core.domain.model.QuotePath
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

typealias HydraDxSwapSourceId = String

class HydraDxSwapEdge(
    override val from: FullChainAssetId,
    val sourceId: HydraDxSwapSourceId,
    val direction: HydraSwapDirection
) : Edge<FullChainAssetId> {

    override val to: FullChainAssetId = direction.to
}

fun Path<HydraDxSwapEdge>.toQuotePath(): QuotePath {
    val segments = map {
        QuotePath.Segment(it.from, it.to, it.sourceId, it.direction.params)
    }

    return QuotePath(segments)
}
