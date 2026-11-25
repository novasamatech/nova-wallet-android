package io.novafoundation.nova.feature_swap_core.domain.paths

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.findDijkstraPathsBetween
import io.novafoundation.nova.common.utils.graph.numberOfEdges
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.measureExecution
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathFeeEstimator
import io.novafoundation.nova.feature_swap_core_api.data.paths.OutputCommission
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.BestPathQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigInteger

private const val PATHS_LIMIT = 10
private const val QUOTES_CACHE = "RealSwapService.QuotesCache"

class RealPathQuoterFactory(
    private val computationalCache: ComputationalCache,
) : PathQuoter.Factory {

    override fun <E : QuotableEdge> create(
        graphFlow: Flow<Graph<FullChainAssetId, E>>,
        computationalScope: CoroutineScope,
        pathFeeEstimation: PathFeeEstimator<E>?,
        filter: EdgeVisitFilter<E>?
    ): PathQuoter<E> {
        return RealPathQuoter(computationalCache, graphFlow, computationalScope, pathFeeEstimation, filter)
    }
}

private class RealPathQuoter<E : QuotableEdge>(
    private val computationalCache: ComputationalCache,
    private val graphFlow: Flow<Graph<FullChainAssetId, E>>,
    private val computationalScope: CoroutineScope,
    private val pathFeeEstimation: PathFeeEstimator<E>?,
    private val filter: EdgeVisitFilter<E>?,
) : PathQuoter<E> {

    // Paths also depend on the account-specific visit filter captured by this quoter.
    private val cacheNamespace = UUID.randomUUID().toString()

    override suspend fun findBestPath(
        chainAssetIn: Chain.Asset,
        chainAssetOut: Chain.Asset,
        amount: BigInteger,
        swapDirection: SwapDirection,
    ): BestPathQuote<E> {
        val from = chainAssetIn.fullId
        val to = chainAssetOut.fullId

        val paths = pathsFromCacheOrCompute(from, to, computationalScope) { graph ->
            val paths = measureExecution("Finding ${chainAssetIn.symbol} -> ${chainAssetOut.symbol} paths") {
                graph.findDijkstraPathsBetween(from, to, limit = PATHS_LIMIT, filter)
            }

            paths
        }

        if (paths.isEmpty()) throw SwapQuoteException.NoRoute

        val results = paths.mapAsync { path ->
            runCatching { quotePath(path, amount, swapDirection) }
                .onFailure {
                    if (it is CancellationException) throw it
                    Log.w("Swaps", "Failed to quote path", it)
                }
        }
        val quotedPaths = results.mapNotNull { it.getOrNull() }

        if (quotedPaths.isEmpty()) {
            // A failed network/runtime request must remain retryable, not look like unavailable liquidity.
            throw results.firstNotNullOfOrNull { result ->
                result.exceptionOrNull()?.takeUnless { it is SwapQuoteException.NotEnoughLiquidity }
            } ?: SwapQuoteException.NotEnoughLiquidity
        }

        return BestPathQuote(quotedPaths)
    }

    private suspend fun pathsFromCacheOrCompute(
        from: FullChainAssetId,
        to: FullChainAssetId,
        scope: CoroutineScope,
        computation: suspend (graph: Graph<FullChainAssetId, E>) -> List<Path<E>>
    ): List<Path<E>> {
        val graph = graphFlow.first()

        val cacheKey = "$QUOTES_CACHE:$cacheNamespace:${pathsCacheKey(from, to)}:${graph.numberOfEdges()}"

        return computationalCache.useCache(cacheKey, scope) {
            computation(graph)
        }
    }

    private fun pathsCacheKey(from: FullChainAssetId, to: FullChainAssetId): String {
        val fromKey = "${from.chainId}:${from.assetId}"
        val toKey = "${to.chainId}:${to.assetId}"

        return "$fromKey:$toKey"
    }

    private suspend fun quotePath(
        path: Path<E>,
        amount: BigInteger,
        swapDirection: SwapDirection
    ): QuotedPath<E> {
        val commission = pathFeeEstimation?.outputCommission(path)
        return when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> quoteSell(path, amount, commission)
            SwapDirection.SPECIFIED_OUT -> quoteBuy(path, amount, commission)
        }
    }

    private suspend fun quoteSell(path: Path<E>, amount: BigInteger, commission: IndexedValue<OutputCommission>?): QuotedPath<E> {
        var available = amount
        val quoted = path.mapIndexed { index, edge ->
            val amounts = edge.quoteAmounts(available, SwapDirection.SPECIFIED_IN, isIntermediate = index > 0)
            val grossOutput = amounts.counterpart
            available = if (commission?.index == index) grossOutput - commission.value.serviceCommissionIncludedIn(grossOutput) else grossOutput
            QuotedEdge(amounts.specified, grossOutput, edge)
        }
        return quoted.withFees(SwapDirection.SPECIFIED_IN, netAmountOut = available)
    }

    private suspend fun quoteBuy(path: Path<E>, amount: BigInteger, commission: IndexedValue<OutputCommission>?): QuotedPath<E> {
        val quoted = ArrayList<QuotedEdge<E>>(path.size)
        var required = amount
        var netAmountOut = amount
        for (index in path.indices.reversed()) {
            val fee = if (commission?.index == index) commission.value.serviceCommissionToAddOnTop(required) else BigInteger.ZERO
            val edge = path[index]
            val amounts = edge.quoteAmounts(required + fee, SwapDirection.SPECIFIED_OUT, isIntermediate = index < path.lastIndex)
            quoted.add(QuotedEdge(amounts.specified, amounts.counterpart, edge))
            if (index == path.lastIndex) netAmountOut = amounts.specified - fee
            required = amounts.counterpart
        }
        return quoted.asReversed().withFees(SwapDirection.SPECIFIED_OUT, netAmountOut)
    }

    private suspend fun Path<QuotedEdge<E>>.withFees(direction: SwapDirection, netAmountOut: BigInteger): QuotedPath<E> {
        val roughFee = pathFeeEstimation?.roughlyEstimateFee(this) ?: PathRoughFeeEstimation.zero()
        return QuotedPath(direction, this, roughFee, netAmountOut)
    }
}
