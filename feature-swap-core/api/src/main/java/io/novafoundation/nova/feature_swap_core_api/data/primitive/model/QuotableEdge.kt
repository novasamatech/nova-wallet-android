package io.novafoundation.nova.feature_swap_core_api.data.primitive.model

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger

interface QuotableEdge : Edge<FullChainAssetId> {

    suspend fun quote(
        amount: BigInteger,
        direction: SwapDirection
    ): BigInteger
}
