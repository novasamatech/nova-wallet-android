package io.novafoundation.nova.feature_account_impl.data.fee.utils

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxAssetConversionFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxSwapEdge
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

class HydraDxQuoteSharedComputation(
    private val computationalCache: ComputationalCache,
    private val assetConversionFactory: HydraDxAssetConversionFactory
) {

    suspend fun directions(
        chain: Chain,
        scope: CoroutineScope
    ): Graph<FullChainAssetId, HydraDxSwapEdge> {
        val key = "HydraDxDirections"

        return computationalCache.useCache(key, scope) {
            val assetConversion = assetConversionFactory.create(chain)
            assetConversion.availableSwapDirections()
        }
    }

    suspend fun quote(
        chain: Chain,
        fromAsset: Chain.Asset,
        toAsset: Chain.Asset,
        scope: CoroutineScope
    ): AssetExchangeQuote {
        val key = "HydraDxQuote"

        return computationalCache.useCache(key, scope) {
            val args = AssetExchangeQuoteArgs(
                chainAssetIn = fromAsset,
                chainAssetOut = toAsset,
                amount = BigInteger.ZERO,
                swapDirection = SwapDirection.SPECIFIED_IN
            )

            val assetConversion = assetConversionFactory.create(chain)
            val swapDirections = directions(chain, scope)
            val paths = assetConversion.getPaths(swapDirections, args)
            assetConversion.quote(paths, args)
        }
    }
}
