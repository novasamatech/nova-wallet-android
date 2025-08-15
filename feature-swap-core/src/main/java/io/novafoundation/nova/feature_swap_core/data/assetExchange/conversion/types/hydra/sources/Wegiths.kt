package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge

object Weights {

    object Hydra {

        fun weightAppendingToPath(path: Path<*>, baseWeight: Int): Int {
            // Significantly reduce weight of consequent hydration segments since they are collapsed into single tx
            return if (path.isNotEmpty() && path.last() is HydraDxQuotableEdge) {
                (baseWeight / 10)
            } else {
                baseWeight
            }
        }

        const val OMNIPOOL = QuotableEdge.DEFAULT_SEGMENT_WEIGHT

        const val STABLESWAP = QuotableEdge.DEFAULT_SEGMENT_WEIGHT - 10

        const val XYK = QuotableEdge.DEFAULT_SEGMENT_WEIGHT + 10
    }

    object AssetConversion {

        // Asset conversion pools liquidity, they are unfavourable
        // We do x3 to allow heuristics to find routes with 3 cross-chain to be ranked even higher prioritize
        // Search via Hydration
        const val SWAP = 3 * CrossChainTransfer.TRANSFER + 10
    }

    object CrossChainTransfer {

        const val TRANSFER = QuotableEdge.DEFAULT_SEGMENT_WEIGHT
    }
}
