package io.novafoundation.nova.feature_swap_impl.domain.swap

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.graph.findAllPossibleDestinations
import io.novafoundation.nova.common.utils.graph.findDijkstraPathsBetween
import io.novafoundation.nova.common.utils.graph.vertices
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.requireInnerNotNull
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requestedAccountPaysFees
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.QuotableEdge
import io.novafoundation.nova.feature_swap_api.domain.model.QuotedSwapEdge
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraph
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapPath
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.BuildConfig
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.ParentQuoterArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

private const val ALL_DIRECTIONS_CACHE = "RealSwapService.ALL_DIRECTIONS"
private const val EXCHANGES_CACHE = "RealSwapService.EXCHANGES"

private const val QUOTES_CACHE = "RealSwapService.QuotesCache"

private const val PATHS_LIMIT = 4

internal class RealSwapService(
    private val assetConversionFactory: AssetConversionExchangeFactory,
    private val hydraDxOmnipoolFactory: HydraDxExchangeFactory,
    private val computationalCache: ComputationalCache,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val debug: Boolean = BuildConfig.DEBUG
) : SwapService {

    override suspend fun canPayFeeInNonUtilityAsset(asset: Chain.Asset): Boolean = withContext(Dispatchers.Default) {
        val computationScope = CoroutineScope(coroutineContext)

        val exchange = exchanges(computationScope).getValue(asset.chainId)
        val isCustomFeeToken = !asset.isCommissionAsset
        val currentMetaAccount = accountRepository.getSelectedMetaAccount()

        // TODO we disable custom fee tokens payment for account types where current account is not the one who pays fees (e.g. it is proxied).
        // This restriction can be removed once we consider all corner-cases
        isCustomFeeToken && exchange.canPayFeeInNonUtilityToken(asset) && currentMetaAccount.type.requestedAccountPaysFees()
    }

    override suspend fun assetsAvailableForSwap(
        computationScope: CoroutineScope
    ): Flow<Set<FullChainAssetId>> {
        return directionsGraph(computationScope).map { it.vertices() }
    }

    override suspend fun availableSwapDirectionsFor(
        asset: Chain.Asset,
        computationScope: CoroutineScope
    ): Flow<Set<FullChainAssetId>> {
        return directionsGraph(computationScope).map { it.findAllPossibleDestinations(asset.fullId) }
    }

    override suspend fun quote(
        args: SwapQuoteArgs,
        computationSharingScope: CoroutineScope
    ): Result<SwapQuote> {
        return withContext(Dispatchers.Default) {
            runCatching {
                quoteInternal(args, computationSharingScope)
            }.onFailure {
                Log.e("RealSwapService", "Error while quoting", it)
            }
        }
    }

    override suspend fun estimateFee(executeArgs: SwapExecuteArgs): SwapFee {
        val atomicOperations = executeArgs.constructAtomicOperations()

        val fees = atomicOperations.mapAsync { it.estimateFee() }

        return SwapFee(fees)
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<SwapExecutionCorrection> {
        val atomicOperations = args.constructAtomicOperations()

        val initialCorrection: Result<SwapExecutionCorrection?> = Result.success(null)

        return atomicOperations.fold(initialCorrection) { prevStepCorrection, operation ->
            prevStepCorrection.flatMap { operation.submit(it) }
        }.requireInnerNotNull()
    }

    private suspend fun SwapExecuteArgs.constructAtomicOperations(): List<AtomicSwapOperation> {
        var currentSwapTx: AtomicSwapOperation? = null
        val finishedSwapTxs = mutableListOf<AtomicSwapOperation>()

        // TODO this will result in lower total slippage if some segments are appendable
        val perSegmentSlippage = slippage / executionPath.size

        executionPath.forEach { segmentExecuteArgs ->
            val quotedEdge = segmentExecuteArgs.quotedSwapEdge

            val operationArgs = AtomicSwapOperationArgs(
                swapLimit = SwapLimit(direction, quotedEdge.quotedAmount, perSegmentSlippage, quotedEdge.quote),
                customFeeAsset = segmentExecuteArgs.customFeeAsset,
            )

            // Initial case - begin first operation
            if (currentSwapTx == null) {
                currentSwapTx = quotedEdge.edge.beginOperation(operationArgs)
                return@forEach
            }

            // Try to append segment to current swap tx
            val maybeAppendedCurrentTx = quotedEdge.edge.appendToOperation(currentSwapTx!!, operationArgs)

            currentSwapTx = if (maybeAppendedCurrentTx == null) {
                finishedSwapTxs.add(currentSwapTx!!)
                quotedEdge.edge.beginOperation(operationArgs)
            } else {
                maybeAppendedCurrentTx
            }
        }

        finishedSwapTxs.add(currentSwapTx!!)

        return finishedSwapTxs
    }

    private suspend fun quoteInternal(
        args: SwapQuoteArgs,
        computationSharingScope: CoroutineScope
    ): SwapQuote {
        val quotedTrade = quoteTrade(
            chainAssetIn = args.tokenIn.configuration,
            chainAssetOut = args.tokenOut.configuration,
            amount = args.amount,
            swapDirection = args.swapDirection,
            computationSharingScope = computationSharingScope
        )

        val amountIn = quotedTrade.amountIn()
        val amountOut = quotedTrade.amountOut()

        return SwapQuote(
            amountIn = args.tokenIn.configuration.withAmount(amountIn),
            amountOut = args.tokenOut.configuration.withAmount(amountOut),
            direction = args.swapDirection,
            priceImpact = args.calculatePriceImpact(amountIn, amountOut),
            path = quotedTrade.path
        )
    }

    override suspend fun slippageConfig(chainId: ChainId): SlippageConfig? {
        val computationScope = CoroutineScope(coroutineContext)
        val exchanges = exchanges(computationScope)
        return exchanges[chainId]?.slippageConfig()
    }

    override fun runSubscriptions(chainIn: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return withFlowScope { scope ->
            val exchanges = exchanges(scope)
            exchanges.getValue(chainIn.id).runSubscriptions(chainIn, metaAccount)
        }.throttleLast(500.milliseconds)
    }

    private fun SwapQuoteArgs.calculatePriceImpact(amountIn: Balance, amountOut: Balance): Percent {
        val fiatIn = tokenIn.planksToFiat(amountIn)
        val fiatOut = tokenOut.planksToFiat(amountOut)

        return calculatePriceImpact(fiatIn, fiatOut)
    }

    private fun QuotedTrade.amountIn(): Balance {
        return when (direction) {
            SwapDirection.SPECIFIED_IN -> firstSegmentQuotedAmount
            SwapDirection.SPECIFIED_OUT -> firstSegmentQuote
        }
    }

    private fun QuotedTrade.amountOut(): Balance {
        return when (direction) {
            SwapDirection.SPECIFIED_IN -> lastSegmentQuote
            SwapDirection.SPECIFIED_OUT -> lastSegmentQuotedAmount
        }
    }

    private fun QuotedTrade.finalQuote(): Balance {
        return when (direction) {
            SwapDirection.SPECIFIED_IN -> lastSegmentQuote
            SwapDirection.SPECIFIED_OUT -> firstSegmentQuote
        }
    }

    private fun calculatePriceImpact(fiatIn: BigDecimal, fiatOut: BigDecimal): Percent {
        if (fiatIn.isZero || fiatOut.isZero) return Percent.zero()

        val priceImpact = (BigDecimal.ONE - fiatOut / fiatIn).atLeastZero()

        return priceImpact.asPerbill().toPercent()
    }

    private suspend fun directionsGraph(computationScope: CoroutineScope): Flow<SwapGraph> {
        return computationalCache.useSharedFlow(ALL_DIRECTIONS_CACHE, computationScope) {
            val exchanges = exchanges(computationScope)

            val directionsByExchange = exchanges.map { (chainId, exchange) ->
                flowOf { exchange.availableDirectSwapConnections() }
                    .catch {
                        emit(emptyList())

                        Log.e("RealSwapService", "Failed to fetch directions for exchange ${exchange::class} in chain $chainId", it)
                    }
            }

            directionsByExchange
                .accumulateLists()
                .filter { it.isNotEmpty() }
                .map { Graph.create(it) }
        }
    }

    private suspend fun exchanges(computationScope: CoroutineScope): Map<ChainId, AssetExchange> {
        return computationalCache.useCache(EXCHANGES_CACHE, computationScope) {
            createExchanges(computationScope)
        }
    }

    private suspend fun createExchanges(coroutineScope: CoroutineScope): Map<ChainId, AssetExchange> {
        return chainRegistry.chainsById.first().mapValues { (_, chain) ->
            createExchange(coroutineScope, chain)
        }
            .filterNotNull()
    }

    private suspend fun createExchange(computationScope: CoroutineScope, chain: Chain): AssetExchange? {
        val factory = when {
            chain.swap.assetConversionSupported() -> assetConversionFactory
            chain.swap.hydraDxSupported() -> hydraDxOmnipoolFactory
            else -> null
        }

        return factory?.create(chain, InnerParentQuoter(computationScope), computationScope)
    }

    // Assumes each flow will have only single element
    private fun <T> List<Flow<List<T>>>.accumulateLists(): Flow<List<T>> {
        return mergeIfMultiple()
            .runningFold(emptyList()) { acc, directions -> acc + directions }
    }

    private suspend fun pathsFromCacheOrCompute(
        from: FullChainAssetId,
        to: FullChainAssetId,
        scope: CoroutineScope,
        computation: suspend () -> List<Path<SwapGraphEdge>>
    ): List<Path<SwapGraphEdge>> {
        val mapKey = from to to
        val cacheKey = "$QUOTES_CACHE:$mapKey"

        return computationalCache.useCache(cacheKey, scope) {
            computation()
        }
    }

    private suspend fun quotePath(
        path: SwapPath,
        amount: Balance,
        swapDirection: SwapDirection
    ): QuotedTrade? {
        val quote = when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> quotePathSell(path, amount)
            SwapDirection.SPECIFIED_OUT -> quotePathBuy(path, amount)
        } ?: return null

        return QuotedTrade(swapDirection, quote)
    }

    private suspend fun quotePathBuy(path: Path<SwapGraphEdge>, amount: Balance): Path<QuotedSwapEdge>? {
        return runCatching {
            val initial = mutableListOf<QuotedSwapEdge>() to amount

            path.foldRight(initial) { segment, (quotedPath, currentAmount) ->
                val segmentQuote = segment.quote(currentAmount, SwapDirection.SPECIFIED_OUT)
                quotedPath.add(0, QuotedSwapEdge(currentAmount, segmentQuote, segment))

                quotedPath to segmentQuote
            }.first
        }.getOrNull()
    }

    private suspend fun quotePathSell(path: Path<SwapGraphEdge>, amount: Balance): Path<QuotedSwapEdge>? {
        return runCatching {
            val initial = mutableListOf<QuotedSwapEdge>() to amount

            path.fold(initial) { (quotedPath, currentAmount), segment ->
                val segmentQuote = segment.quote(currentAmount, SwapDirection.SPECIFIED_IN)
                quotedPath.add(QuotedSwapEdge(currentAmount, segmentQuote, segment))

                quotedPath to segmentQuote
            }.first
        }.getOrNull()
    }

    private suspend fun quoteTrade(
        chainAssetIn: Chain.Asset,
        chainAssetOut: Chain.Asset,
        amount: Balance,
        swapDirection: SwapDirection,
        computationSharingScope: CoroutineScope
    ): QuotedTrade {
        val from = chainAssetIn.fullId
        val to = chainAssetOut.fullId

        val paths = pathsFromCacheOrCompute(from, to, computationSharingScope) {
            val graph = directionsGraph(computationSharingScope).first()

            graph.findDijkstraPathsBetween(from, to, limit = PATHS_LIMIT)
        }

        val quotedPaths = paths.mapNotNull { path -> quotePath(path, amount, swapDirection) }
        if (paths.isEmpty()) {
            throw SwapQuoteException.NotEnoughLiquidity
        }

        return quotedPaths.max()
    }

    private inner class InnerParentQuoter(
        private val computationScope: CoroutineScope
    ) : AssetExchange.ParentQuoter {

        override suspend fun quote(quoteArgs: ParentQuoterArgs): Balance {
            return quoteTrade(
                chainAssetIn = quoteArgs.chainAssetIn,
                chainAssetOut = quoteArgs.chainAssetOut,
                amount = quoteArgs.amount,
                swapDirection = quoteArgs.swapDirection,
                computationSharingScope = computationScope
            ).finalQuote()
        }
    }

    // TOOD rework path logging
//    private suspend fun logQuotes(args: SwapQuoteArgs, quotes: List<AssetExchangeQuote>) {
//        val allCandidates = quotes.sortedDescending().map {
//            val formattedIn = args.amount.formatPlanks(args.tokenIn.configuration)
//            val formattedOut = it.quote.formatPlanks(args.tokenOut.configuration)
//            val formattedPath = formatPath(it.path)
//
//            "$formattedIn to $formattedOut via $formattedPath"
//        }.joinToString(separator = "\n")
//
//        Log.d("RealSwapService", "-------- New quote ----------")
//        Log.d("RealSwapService", allCandidates)
//        Log.d("RealSwapService", "-------- Done quote ----------\n\n\n")
//    }
//
//    private suspend fun formatPath(path: QuotePath): String {
//        val assets = chain.assetsById
//
//        return buildString {
//            val firstSegment = path.segments.first()
//
//            append(assets.getValue(firstSegment.from.assetId).symbol)
//
//            append("  -- ${formatSource(firstSegment)} -->  ")
//
//            append(assets.getValue(firstSegment.to.assetId).symbol)
//
//            path.segments.subList(1, path.segments.size).onEach { segment ->
//                append("  -- ${formatSource(segment)} -->  ")
//
//                append(assets.getValue(segment.to.assetId).symbol)
//            }
//        }
//    }
//
//    private suspend fun formatSource(segment: QuotePath.Segment): String {
//        return buildString {
//            append(segment.sourceId)
//
//            if (segment.sourceId == StableSwapSourceFactory.ID) {
//                val onChainId = segment.sourceParams.getValue("PoolId").toBigInteger()
//                val chainAsset = hydraDxAssetIdConverter.toChainAssetOrThrow(chain, onChainId)
//                append("[${chainAsset.symbol}]")
//            }
//        }
//    }
}

abstract class BaseSwapGraphEdge(
    val fromAsset: Chain.Asset,
    val toAsset: Chain.Asset
) : SwapGraphEdge {

    final override val from: FullChainAssetId = fromAsset.fullId

    final override val to: FullChainAssetId = toAsset.fullId
}


abstract class BaseQuotableEdge(
    val fromAsset: Chain.Asset,
    val toAsset: Chain.Asset
) : QuotableEdge {

    final override val from: FullChainAssetId = fromAsset.fullId

    final override val to: FullChainAssetId = toAsset.fullId
}

private class QuotedTrade(
    val direction: SwapDirection,
    val path: Path<QuotedSwapEdge>
) : Comparable<QuotedTrade> {

    override fun compareTo(other: QuotedTrade): Int {
        return when (direction) {
            // When we want to sell a token, the bigger the quote - the better
            SwapDirection.SPECIFIED_IN -> (lastSegmentQuote - other.lastSegmentQuote).signum()
            // When we want to buy a token, the smaller the quote - the better
            SwapDirection.SPECIFIED_OUT -> (other.firstSegmentQuote - firstSegmentQuote).signum()
        }
    }
}

private val QuotedTrade.lastSegmentQuotedAmount: Balance
    get() = path.last().quotedAmount

private val QuotedTrade.lastSegmentQuote: Balance
    get() = path.last().quote

private val QuotedTrade.firstSegmentQuote: Balance
    get() = path.first().quote

private val QuotedTrade.firstSegmentQuotedAmount: Balance
    get() = path.first().quotedAmount
