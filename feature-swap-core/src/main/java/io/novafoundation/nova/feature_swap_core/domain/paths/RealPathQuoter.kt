package io.novafoundation.nova.feature_swap_core.domain.paths

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.findDijkstraPathsBetween
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.measureExecution
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.BestPathQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import java.math.BigInteger

private const val PATHS_LIMIT = 4
private const val QUOTES_CACHE = "RealSwapService.QuotesCache"

class RealPathQuoterFactory(
    private val computationalCache: ComputationalCache,
) : PathQuoter.Factory {

    override fun <E : QuotableEdge> create(
        graph: Graph<FullChainAssetId, E>,
        computationalScope: CoroutineScope,
        filter: EdgeVisitFilter<E>?
    ): PathQuoter<E> {
        return RealPathQuoter(computationalCache, graph, computationalScope, filter)
    }
}

private class RealPathQuoter<E : QuotableEdge>(
    private val computationalCache: ComputationalCache,
    private val graph: Graph<FullChainAssetId, E>,
    private val computationalScope: CoroutineScope,
    private val filter: EdgeVisitFilter<E>?
) : PathQuoter<E> {

    override suspend fun findBestPath(
        chainAssetIn: Chain.Asset,
        chainAssetOut: Chain.Asset,
        amount: BigInteger,
        swapDirection: SwapDirection,
    ): BestPathQuote<E> {
        val from = chainAssetIn.fullId
        val to = chainAssetOut.fullId

        val paths = pathsFromCacheOrCompute(from, to, computationalScope) {
            val paths = measureExecution("Finding ${chainAssetIn.symbol} -> ${chainAssetOut.symbol} paths") {
                graph.findDijkstraPathsBetween(from, to, limit = PATHS_LIMIT, filter)
            }

            paths
        }

        val quotedPaths = paths.mapAsync { path -> quotePath(path, amount, swapDirection) }
            .filterNotNull()

        if (quotedPaths.isEmpty()) {
            throw SwapQuoteException.NotEnoughLiquidity
        }

        return BestPathQuote(quotedPaths)
    }

    private suspend fun pathsFromCacheOrCompute(
        from: FullChainAssetId,
        to: FullChainAssetId,
        scope: CoroutineScope,
        computation: suspend () -> List<Path<E>>
    ): List<Path<E>> {
        val cacheKey = "$QUOTES_CACHE:${pathsCacheKey(from, to)}"

        return computationalCache.useCache(cacheKey, scope) {
            computation()
        }
    }

    private fun pathsCacheKey(from: FullChainAssetId, to: FullChainAssetId): String {
        val fromKey = "${from.chainId}:${from.assetId}"
        val toKey = "${to.chainId}:${to.assetId}"

        return "${fromKey}:${toKey}"
    }

    private suspend fun quotePath(
        path: Path<E>,
        amount: BigInteger,
        swapDirection: SwapDirection
    ): QuotedPath<E>? {
        val quote = when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> quotePathSell(path, amount)
            SwapDirection.SPECIFIED_OUT -> quotePathBuy(path, amount)
        } ?: return null

        return QuotedPath(swapDirection, quote)
    }

    private suspend fun quotePathBuy(path: Path<E>, amount: BigInteger): Path<QuotedEdge<E>>? {
        return runCatching {
            val initial = mutableListOf<QuotedEdge<E>>() to amount

            path.foldRight(initial) { segment, (quotedPath, currentAmount) ->
                val segmentQuote = segment.quote(currentAmount, SwapDirection.SPECIFIED_OUT)
                quotedPath.add(0, QuotedEdge(currentAmount, segmentQuote, segment))

                quotedPath to segmentQuote
            }.first
        }
            .onFailure { Log.w("Swaps", "Failed to quote path", it) }
            .getOrNull()
    }

    private suspend fun quotePathSell(path: Path<E>, amount: BigInteger): Path<QuotedEdge<E>>? {
        return runCatching {
            val initial = mutableListOf<QuotedEdge<E>>() to amount

            path.fold(initial) { (quotedPath, currentAmount), segment ->
                val segmentQuote = segment.quote(currentAmount, SwapDirection.SPECIFIED_IN)
                quotedPath.add(QuotedEdge(currentAmount, segmentQuote, segment))

                quotedPath to segmentQuote
            }.first
        }
            .onFailure { Log.w("Swaps", "Failed to quote path", it) }
            .getOrNull()
    }
}


