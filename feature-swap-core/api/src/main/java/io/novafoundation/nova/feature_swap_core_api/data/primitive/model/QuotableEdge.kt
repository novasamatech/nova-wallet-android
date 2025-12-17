package io.novafoundation.nova.feature_swap_core_api.data.primitive.model

import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger

interface QuotableEdge : WeightedEdge<FullChainAssetId> {
    companion object {

        // Allow [0..100] precision for smaller weights
        const val DEFAULT_SEGMENT_WEIGHT = 100
    }

    suspend fun quote(
        amount: BigInteger,
        direction: SwapDirection
    ): BigInteger
}
