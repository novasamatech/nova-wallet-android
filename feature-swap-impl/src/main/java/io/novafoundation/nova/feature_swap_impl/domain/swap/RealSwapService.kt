package io.novafoundation.nova.feature_swap_impl.domain.swap

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.graph.findAllPossibleDestinations
import io.novafoundation.nova.common.utils.graph.hasOutcomingDirections
import io.novafoundation.nova.common.utils.graph.vertices
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.requireInnerNotNull
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraph
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.firstSegmentQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.firstSegmentQuotedAmount
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.lastSegmentQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.lastSegmentQuotedAmount
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.BuildConfig
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.ParentQuoterArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.compound.CompoundAssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain.CrossChainTransferAssetExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
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
import kotlin.time.Duration.Companion.milliseconds

private const val ALL_DIRECTIONS_CACHE = "RealSwapService.ALL_DIRECTIONS"
private const val EXCHANGES_CACHE = "RealSwapService.EXCHANGES"
private const val QUOTER_CACHE = "RealSwapService.QUOTER"


internal class RealSwapService(
    private val assetConversionFactory: AssetConversionExchangeFactory,
    private val hydraDxExchangeFactory: HydraDxExchangeFactory,
    private val crossChainTransferFactory: CrossChainTransferAssetExchangeFactory,
    private val computationalCache: ComputationalCache,
    private val chainRegistry: ChainRegistry,
    private val quoterFactory: PathQuoter.Factory,
    private val customFeeCapabilityFacade: CustomFeeCapabilityFacade,
    private val debug: Boolean = BuildConfig.DEBUG
) : SwapService {

    override suspend fun canPayFeeInNonUtilityAsset(asset: Chain.Asset): Boolean = withContext(Dispatchers.Default) {
        val computationScope = CoroutineScope(coroutineContext)

        val exchange = exchangeRegistry(computationScope).getExchange(asset.chainId)
        customFeeCapabilityFacade.canPayFeeInNonUtilityToken(asset, exchange)
    }

    override suspend fun sync(coroutineScope: CoroutineScope) {
        Log.d("Swaps", "Syncing swap service")

        exchangeRegistry(coroutineScope)
            .allExchanges()
            .forEachAsync { it.sync() }
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
        return directionsGraph(computationScope).map {
            it.findAllPossibleDestinations(asset.fullId)
        }
    }

    override suspend fun hasAvailableSwapDirections(asset: Chain.Asset, computationScope: CoroutineScope): Flow<Boolean> {
        return directionsGraph(computationScope).map { it.hasOutcomingDirections(asset.fullId) }
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
                // TODO custom fee assets
                customFeeAsset = null,
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
            priceImpact = args.calculatePriceImpact(amountIn, amountOut),
            quotedPath = quotedTrade
        )
    }

    override suspend fun defaultSlippageConfig(chainId: ChainId): SlippageConfig {
        return SlippageConfig.default()
    }

    override fun runSubscriptions( metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return withFlowScope { scope ->
            val exchangeRegistry = exchangeRegistry(scope)

            exchangeRegistry.allExchanges()
                .map { it.runSubscriptions(metaAccount) }
                .mergeIfMultiple()
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
            val exchangeRegistry = exchangeRegistry(computationScope)

            val directionsByExchange = exchangeRegistry.allExchanges().map { exchange ->
                flowOf { exchange.availableDirectSwapConnections() }
                    .catch {
                        emit(emptyList())

                        Log.e("RealSwapService", "Failed to fetch directions for exchange ${exchange::class}", it)
                    }
            }

            directionsByExchange
                .accumulateLists()
                .filter { it.isNotEmpty() }
                .map { Graph.create(it) }
        }
    }

    private suspend fun exchangeRegistry(computationScope: CoroutineScope): ExchangeRegistry {
        return computationalCache.useCache(EXCHANGES_CACHE, computationScope) {
            createExchangeRegistry(this)
        }
    }

    private suspend fun createExchangeRegistry(coroutineScope: CoroutineScope): ExchangeRegistry {
        return ExchangeRegistry(
            singleChainExchanges = createIndividualChainExchanges(coroutineScope),
            multiChainExchanges = listOf(
                crossChainTransferFactory.create(InnerParentQuoter(coroutineScope), coroutineScope)
            )
        )
    }

    private suspend fun createIndividualChainExchanges(coroutineScope: CoroutineScope): Map<ChainId, AssetExchange> {
        return chainRegistry.chainsById.first().mapValues { (_, chain) ->
            createSingleExchange(coroutineScope, chain)
        }
            .filterNotNull()
    }

    private suspend fun createSingleExchange(computationScope: CoroutineScope, chain: Chain): AssetExchange? {
        val factory = when {
            chain.swap.assetConversionSupported() -> assetConversionFactory
            chain.swap.hydraDxSupported() -> hydraDxExchangeFactory
            else -> null
        }

        return factory?.create(chain, InnerParentQuoter(computationScope), computationScope)
    }

    // Assumes each flow will have only single element
    private fun <T> List<Flow<List<T>>>.accumulateLists(): Flow<List<T>> {
        return mergeIfMultiple()
            .runningFold(emptyList()) { acc, directions -> acc + directions }
    }


    private suspend fun quoteTrade(
        chainAssetIn: Chain.Asset,
        chainAssetOut: Chain.Asset,
        amount: Balance,
        swapDirection: SwapDirection,
        computationSharingScope: CoroutineScope
    ): QuotedTrade {
        val quoter = getPathQuoter(computationSharingScope)

        val bestPathQuote = quoter.findBestPath(chainAssetIn, chainAssetOut, amount, swapDirection)
        if (debug) {
            logQuotes(bestPathQuote.candidates)
        }

        return bestPathQuote.bestPath
    }

    private suspend fun getPathQuoter(computationScope: CoroutineScope): PathQuoter<SwapGraphEdge> {
        return computationalCache.useCache(QUOTER_CACHE, computationScope) {
            val graph = directionsGraph(computationScope).first()
            quoterFactory.create(graph, computationScope)
        }
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

    private suspend fun logQuotes(quotedTrades: List<QuotedTrade>) {
        val allCandidates = quotedTrades.sortedDescending()
            .map { trade -> formatTrade(trade) }
            .joinToString(separator = "\n")

        Log.d("RealSwapService", "-------- New quote ----------")
        Log.d("RealSwapService", allCandidates)
        Log.d("RealSwapService", "-------- Done quote ----------\n\n\n")
    }

    private suspend fun formatTrade(trade: QuotedTrade): String {
        return buildString {
            trade.path.onEachIndexed { index, quotedSwapEdge ->
                if (index == 0) {
                    val assetIn = chainRegistry.asset(quotedSwapEdge.edge.from)
                    val initialAmount = quotedSwapEdge.quotedAmount.formatPlanks(assetIn)
                    append(initialAmount)
                }

                append(" --- " + quotedSwapEdge.edge.debugLabel() + " ---> ")

                val assetOut = chainRegistry.asset(quotedSwapEdge.edge.to)
                val outAmount = quotedSwapEdge.quote.formatPlanks(assetOut)

                append(outAmount)
            }
        }
    }

    private class ExchangeRegistry(
        private val singleChainExchanges: Map<ChainId, AssetExchange>,
        private val multiChainExchanges: List<AssetExchange>,
    ) {

        fun getExchange(chainId: ChainId): AssetExchange {
            val relevantExchanges = buildList {
                singleChainExchanges[chainId]?.let { add(it) }
                addAll(multiChainExchanges)
            }

            return when(relevantExchanges.size) {
                0 -> error("No exchanges found")
                1 -> relevantExchanges.single()
                else -> CompoundAssetExchange(relevantExchanges)
            }
        }

        fun allExchanges(): List<AssetExchange> {
            return buildList {
                addAll(singleChainExchanges.values)
                addAll(multiChainExchanges)
            }
        }
    }
}

private typealias QuotedTrade = QuotedPath<SwapGraphEdge>

abstract class BaseSwapGraphEdge(
    val fromAsset: Chain.Asset,
    val toAsset: Chain.Asset
) : SwapGraphEdge {

    final override val from: FullChainAssetId = fromAsset.fullId

    final override val to: FullChainAssetId = toAsset.fullId
}
