package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.MutableMultiMap
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.mutableMultiMapOf
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_swap_api.domain.model.SwapArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

private const val ALL_DIRECTIONS_CACHE = "RealSwapService.ALL_DIRECTIONS"
private const val EXCHANGES_CACHE = "RealSwapService.EXCHANGES"

internal class RealSwapService(
    private val assetConversionFactory: AssetConversionExchangeFactory,
    private val computationalCache: ComputationalCache,
) : SwapService {

    override suspend fun assetsAvailableForSwap(
        computationScope: CoroutineScope
    ): Set<FullChainAssetId> = withContext(Dispatchers.Default) {
        allAvailableDirections(computationScope).keys
    }

    override suspend fun availableSwapDirectionsFor(
        asset: Chain.Asset,
        computationScope: CoroutineScope
    ): Set<FullChainAssetId> = withContext(Dispatchers.Default) {
        allAvailableDirections(computationScope)[asset.fullId].orEmpty()
    }

    override suspend fun quote(args: SwapArgs): Result<SwapQuote> {
        val computationScope = CoroutineScope(coroutineContext)

        return runCatching { exchanges(computationScope).getValue(args.assetIn.chainId) }
            .flatMap { exchange -> exchange.quote(args) }
    }

    override suspend fun swap(args: SwapArgs): Result<ExtrinsicHash> {
        val computationScope = CoroutineScope(coroutineContext)

        return runCatching { exchanges(computationScope).getValue(args.assetIn.chainId) }
            .flatMap { exchange -> exchange.swap(args) }
    }

    private suspend fun allAvailableDirections(computationScope: CoroutineScope): MultiMap<FullChainAssetId, FullChainAssetId> {
        return computationalCache.useCache(ALL_DIRECTIONS_CACHE, computationScope) {
            val exchanges = exchanges(computationScope)

            val directionsByExchange = exchanges.map { (_, exchange) ->
                async { exchange.availableSwapDirections() }
            }.awaitAll()

            directionsByExchange.fold(mutableMultiMapOf()) { acc, directions ->
                // MultiMap is not castable to MultiMap in general but its safe here since we don't access inner MutableSet
                @Suppress("UNCHECKED_CAST")
                acc.putAll(directions as MutableMultiMap<FullChainAssetId, FullChainAssetId>)
                acc
            }
        }
    }

    private suspend fun exchanges(computationScope: CoroutineScope): Map<ChainId, AssetExchange> {
        return computationalCache.useCache(EXCHANGES_CACHE, computationScope) {
            createExchanges()
        }
    }

    private suspend fun createExchanges(): Map<ChainId, AssetExchange> {
        return listOfNotNull(
            assetConversionFactory inChain Chain.Geneses.STATEMINE,
            assetConversionFactory inChain Chain.Geneses.STATEMINT,
            assetConversionFactory inChain Chain.Geneses.WESTMINT
        ).toMap()
    }

    private suspend infix fun AssetExchange.Factory.inChain(chainId: ChainId): Pair<ChainId, AssetExchange>? {
        return create(chainId)?.let { chainId to it }
    }
}
