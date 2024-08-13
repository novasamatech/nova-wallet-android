package io.novafoundation.nova.feature_account_impl.data.fee.utils

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.common.utils.graph.Path
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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

    suspend fun paths(
        chain: Chain,
        args: AssetExchangeQuoteArgs,
        accountId: AccountId,
        scope: CoroutineScope
    ): List<Path<HydraDxSwapEdge>> {
        val key = "HydraDxPaths:${accountId.toHexString()}"

        return computationalCache.useCache(key, scope) {
            val assetConversion = getAssetConversion(chain, accountId, scope)
            val swapDirections = directions(chain, accountId, scope)
            assetConversion.getPaths(swapDirections, args)
        }
    }

    suspend fun quote(
        chain: Chain,
        accountId: AccountId,
        fromAsset: Chain.Asset,
        toAsset: Chain.Asset,
        amount: BigInteger,
        scope: CoroutineScope
    ): AssetExchangeQuote {
        val args = AssetExchangeQuoteArgs(
            chainAssetIn = fromAsset,
            chainAssetOut = toAsset,
            amount = BigInteger.ZERO,
            swapDirection = SwapDirection.SPECIFIED_IN
        )

        val assetConversion = getAssetConversion(chain, accountId, scope)
        val swapDirections = directions(chain, accountId, scope)
        val paths = assetConversion.getPaths(swapDirections, args)
        return assetConversion.quote(paths, args)
    }

    suspend fun getAssetConversion(
        chain: Chain,
        accountId: AccountId,
        scope: CoroutineScope
    ): AssetConversion<HydraDxSwapEdge> {
        val key = "HydraDxAssetConversion:${accountId.toHexString()}"

        return computationalCache.useCache(key, scope) {
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)
            val assetConversion = assetConversionFactory.create(chain)

            scope.launch {
                assetConversion.runSubscriptions(accountId, subscriptionBuilder)
                    .throttleLast(500.milliseconds)
                    .launchIn(scope)

                subscriptionBuilder.subscribe(scope)
            }

            assetConversion
        }
    }
}
