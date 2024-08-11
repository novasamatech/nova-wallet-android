package io.novafoundation.nova.feature_account_impl.data.fee.utils

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetConversion
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxAssetConversionFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxSwapEdge
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

class HydraDxQuoteSharedComputation(
    private val computationalCache: ComputationalCache,
    private val assetConversionFactory: HydraDxAssetConversionFactory,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory
) {

    suspend fun directions(
        chain: Chain,
        accountId: AccountId,
        scope: CoroutineScope
    ): Graph<FullChainAssetId, HydraDxSwapEdge> {
        val key = "HydraDxDirections:${accountId.toHexString()}"

        return computationalCache.useCache(key, scope) {
            val assetConversion = getAssetConversion(chain, accountId, scope)
            assetConversion.availableSwapDirections()
        }
    }

    suspend fun quote(
        chain: Chain,
        accountId: AccountId,
        fromAsset: Chain.Asset,
        toAsset: Chain.Asset,
        scope: CoroutineScope
    ): AssetExchangeQuote {
        val key = "HydraDxQuote:${accountId.toHexString()}"

        return computationalCache.useCache(key, scope) {
            val args = AssetExchangeQuoteArgs(
                chainAssetIn = fromAsset,
                chainAssetOut = toAsset,
                amount = BigInteger.ZERO,
                swapDirection = SwapDirection.SPECIFIED_IN
            )

            val assetConversion = getAssetConversion(chain, accountId, scope)
            val swapDirections = directions(chain, accountId, scope)
            val paths = assetConversion.getPaths(swapDirections, args)
            assetConversion.quote(paths, args)
        }
    }

    private suspend fun getAssetConversion(
        chain: Chain,
        accountId: AccountId,
        scope: CoroutineScope
    ): AssetConversion<HydraDxSwapEdge> {
        val key = "HydraDxAssetConversion:${accountId.toHexString()}"

        return computationalCache.useCache(key, scope) {
            val storageSharedRequestBuilder = storageSharedRequestsBuilderFactory.create(chain.id)
            val assetConversion = assetConversionFactory.create(chain)
            assetConversion.runSubscriptions(accountId, storageSharedRequestBuilder)
            assetConversion
        }
    }
}
