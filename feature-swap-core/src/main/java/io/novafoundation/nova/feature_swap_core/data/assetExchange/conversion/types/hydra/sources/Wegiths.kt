package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources

import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge

object Weights {

    object Hydra {

        const val OMNIPOOL = QuotableEdge.DEFAULT_SEGMENT_WEIGHT

        const val STABLESWAP = QuotableEdge.DEFAULT_SEGMENT_WEIGHT - 1

        const val XYK = QuotableEdge.DEFAULT_SEGMENT_WEIGHT + 1

        const val AAVE = STABLESWAP
    }

    object AssetConversion {

        const val SWAP = QuotableEdge.DEFAULT_SEGMENT_WEIGHT + 2
    }

    object CrossChainTransfer {

        const val TRANSFER = QuotableEdge.DEFAULT_SEGMENT_WEIGHT
    }
}
