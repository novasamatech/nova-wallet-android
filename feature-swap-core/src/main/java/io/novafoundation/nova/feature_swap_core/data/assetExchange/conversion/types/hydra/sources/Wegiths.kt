package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge

object Weights {

    object Hydra {

        fun weightAppendingToPath(path: Path<*>, baseWeight: Int): Int {
            // Significantly reduce weight of consequent hydration segments since they are collapsed into single tx
            return if (path.isNotEmpty() && path.last() is HydraDxQuotableEdge) {
                // We divide here by 10 to achieve two goals:
                // 1. Divisor should be significant enough to allow multiple appended segments to be added without influencing total hydration weight much
                // 2. On the other hand, divisor cannot be extremely large as we will loose precision and it wont be possible
                // to distinguish different hydration segments weights between each other.
                // That is also why OMNIPOOL, STABLESWAP and XYK differ by a multiple of ten
                (baseWeight / 10)
            } else {
                baseWeight
            }
        }

        const val OMNIPOOL = QuotableEdge.DEFAULT_SEGMENT_WEIGHT

        const val STABLESWAP = QuotableEdge.DEFAULT_SEGMENT_WEIGHT - 10

        const val XYK = QuotableEdge.DEFAULT_SEGMENT_WEIGHT + 10

        const val AAVE = STABLESWAP
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
