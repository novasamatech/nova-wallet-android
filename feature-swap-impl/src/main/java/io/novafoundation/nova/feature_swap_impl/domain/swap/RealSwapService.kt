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
import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.graph.findAllPossibleDestinations
import io.novafoundation.nova.common.utils.graph.hasOutcomingDirections
import io.novafoundation.nova.common.utils.graph.vertices
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraph
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.amountToLeaveOnOriginToPayTxFees
import io.novafoundation.nova.feature_swap_api.domain.model.replaceAmountIn
import io.novafoundation.nova.feature_swap_api.domain.model.totalFeeEnsuringSubmissionAsset
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
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.ParentQuoterArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain.CrossChainTransferAssetExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.ext.isUtility
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration.Companion.milliseconds

private const val ALL_DIRECTIONS_CACHE = "RealSwapService.ALL_DIRECTIONS"
private const val EXCHANGES_CACHE = "RealSwapService.EXCHANGES"
private const val EXTRINSIC_SERVICE_CACHE = "RealSwapService.ExtrinsicService"
private const val QUOTER_CACHE = "RealSwapService.QUOTER"
private const val NODE_VISIT_FILTER = "RealSwapService.NodeVisitFilter"


internal class RealSwapService(
    private val assetConversionFactory: AssetConversionExchangeFactory,
    private val hydraDxExchangeFactory: HydraDxExchangeFactory,
    private val crossChainTransferFactory: CrossChainTransferAssetExchangeFactory,
    private val computationalCache: ComputationalCache,
    private val chainRegistry: ChainRegistry,
    private val quoterFactory: PathQuoter.Factory,
    private val customFeeCapabilityFacade: CustomFeeCapabilityFacade,
    private val extrinsicServiceFactory: ExtrinsicService.Factory,
    private val defaultFeePaymentProviderRegistry: FeePaymentProviderRegistry,
    private val debug: Boolean = BuildConfig.DEBUG
) : SwapService {

    override suspend fun canPayFeeInNonUtilityAsset(asset: Chain.Asset): Boolean = withContext(Dispatchers.Default) {
        val computationScope = CoroutineScope(coroutineContext)
        val exchangeRegistry = exchangeRegistry(computationScope)
        val paymentRegistry = exchangeRegistry.getFeePaymentRegistry()

        val chain = chainRegistry.getChain(asset.chainId)
        val feePayment = paymentRegistry.providerFor(chain.id).feePaymentFor(asset.toFeePaymentCurrency(), computationScope)

        customFeeCapabilityFacade.canPayFeeInNonUtilityToken(asset, feePayment)
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
            val filter = canPayFeeNodeFilter(computationScope)
            it.findAllPossibleDestinations(asset.fullId, filter) - asset.fullId
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

    override suspend fun estimateFee(executeArgs: SwapFeeArgs): SwapFee {
        val atomicOperations = executeArgs.constructAtomicOperations()

        val fees = atomicOperations.mapAsync { SwapFee.SwapSegment(it.estimateFee(), it) }
        val convertedFees = fees.convertIntermediateSegmentsFeesToAssetIn(executeArgs.assetIn)

        return SwapFee(segments = fees, intermediateSegmentFeesInAssetIn = convertedFees).also(::logFee)
    }

    override suspend fun swap(calculatedFee: SwapFee): Flow<SwapProgress> {
        val atomicOperations = calculatedFee.segments

        val initialCorrection: Result<SwapExecutionCorrection?> = Result.success(null)

        return flow {
            // Zip assumes atomicOperations and atomicOperationFees were constructed the same way
            atomicOperations.fold(initialCorrection) { prevStepCorrection, (segmentFee, operation) ->
                prevStepCorrection.flatMap { correction ->
                    emit(SwapProgress.StepStarted(operation.inProgressLabel()))

                    val newAmountIn = if (correction != null) {
                        correction.actualReceivedAmount - segmentFee.amountToLeaveOnOriginToPayTxFees()
                    } else {
                        val amountIn = operation.estimatedSwapLimit.estimatedAmountIn()
                        amountIn + calculatedFee.additionalAmountForSwap.amount
                    }

                    val actualSwapLimit = operation.estimatedSwapLimit.replaceAmountIn(newAmountIn)
                    val segmentSubmissionArgs = AtomicSwapOperationSubmissionArgs(actualSwapLimit)

                    Log.d("SwapSubmission", operation.inProgressLabel() + " with $actualSwapLimit")

                    operation.submit(segmentSubmissionArgs).onFailure {
                        Log.e("SwapSubmission", "Swap failed on stage '${operation.inProgressLabel()}'", it)

                        emit(SwapProgress.Failure(it))
                    }
                }
            }.onSuccess {
                emit(SwapProgress.Done)
            }
        }
    }

    private fun SwapLimit.estimatedAmountIn(): Balance {
        return when (this) {
            is SwapLimit.SpecifiedIn -> amountIn
            is SwapLimit.SpecifiedOut -> amountInQuote
        }
    }

    private suspend fun List<SwapFee.SwapSegment>.convertIntermediateSegmentsFeesToAssetIn(assetIn: Chain.Asset): FeeBase {
        val convertedFees = foldRightIndexed(BigInteger.ZERO) { index, (operationFee, swapOperation), futureFeePlanks ->
            val amountInToGetFeesForOut = if (futureFeePlanks.isPositive()) {
                swapOperation.requiredAmountInToGetAmountOut(futureFeePlanks)
            } else {
                BigInteger.ZERO
            }

            amountInToGetFeesForOut + if (index != 0) {
                // Ensure everything is in the same asset
                operationFee.totalFeeEnsuringSubmissionAsset()
            } else {
                // First segment is not included
                BigInteger.ZERO
            }
        }

        return SubstrateFeeBase(convertedFees, assetIn)
    }

    private suspend fun SwapFeeArgs.constructAtomicOperations(): List<AtomicSwapOperation> {
        var currentSwapTx: AtomicSwapOperation? = null
        val finishedSwapTxs = mutableListOf<AtomicSwapOperation>()

        executionPath.forEachIndexed { index, segmentExecuteArgs ->
            val quotedEdge = segmentExecuteArgs.quotedSwapEdge

            val operationArgs = AtomicSwapOperationArgs(
                estimatedSwapLimit = SwapLimit(direction, quotedEdge.quotedAmount, slippage, quotedEdge.quote),
                feePaymentCurrency = segmentExecuteArgs.quotedSwapEdge.edge.identifySegmentCurrency(
                    isFirstSegment = index == 0,
                    firstSegmentFees = firstSegmentFees,
                ),
            )

            // Initial case - begin first operation
            if (currentSwapTx == null) {
                currentSwapTx = quotedEdge.edge.beginOperation(operationArgs)
                return@forEachIndexed
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

    private suspend fun SwapGraphEdge.identifySegmentCurrency(
        isFirstSegment: Boolean,
        firstSegmentFees: Chain.Asset
    ): FeePaymentCurrency {
        return if (isFirstSegment) {
            firstSegmentFees.toFeePaymentCurrency()
        } else {
            // When executing intermediate segments, always pay in sending asset
            chainRegistry.asset(from).toFeePaymentCurrency()
        }
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

    override fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return withFlowScope { scope ->
            val exchangeRegistry = exchangeRegistry(scope)

            exchangeRegistry.allExchanges()
                .map { it.runSubscriptions(metaAccount) }
                .mergeIfMultiple()
        }.debounce(500.milliseconds)
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


    private suspend fun canPayFeeNodeFilter(computationScope: CoroutineScope): EdgeVisitFilter<SwapGraphEdge> {
        return computationalCache.useCache(NODE_VISIT_FILTER, computationScope) {
            CanPayFeeNodeVisitFilter(this)
        }
    }

    private suspend fun extrinsicService(computationScope: CoroutineScope): ExtrinsicService {
        return computationalCache.useCache(EXTRINSIC_SERVICE_CACHE, computationScope) {
            createExtrinsicService(this)
        }
    }

    private suspend fun createExchangeRegistry(coroutineScope: CoroutineScope): ExchangeRegistry {
        return ExchangeRegistry(
            singleChainExchanges = createIndividualChainExchanges(coroutineScope),
            multiChainExchanges = listOf(
                crossChainTransferFactory.create(InnerSwapHost(coroutineScope), coroutineScope)
            )
        )
    }

    private suspend fun createExtrinsicService(coroutineScope: CoroutineScope): ExtrinsicService {
        val exchangeRegistry = exchangeRegistry(coroutineScope)
        val feePaymentRegistry = exchangeRegistry.getFeePaymentRegistry()

        return extrinsicServiceFactory.create(
            ExtrinsicService.FeePaymentConfig(
                coroutineScope = coroutineScope,
                customFeePaymentRegistry = feePaymentRegistry
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

        return factory?.create(chain, InnerSwapHost(computationScope), computationScope)
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
        computationSharingScope: CoroutineScope,
        logQuotes: Boolean = true
    ): QuotedTrade {
        val quoter = getPathQuoter(computationSharingScope)

        val bestPathQuote = quoter.findBestPath(chainAssetIn, chainAssetOut, amount, swapDirection)
        if (debug && logQuotes) {
            logQuotes(bestPathQuote.candidates)
        }

        return bestPathQuote.bestPath
    }

    private suspend fun getPathQuoter(computationScope: CoroutineScope): PathQuoter<SwapGraphEdge> {
        return computationalCache.useCache(QUOTER_CACHE, computationScope) {
            val graph = directionsGraph(computationScope).first()
            val filter = canPayFeeNodeFilter(computationScope)

            quoterFactory.create(graph, this, filter)
        }
    }

    private inner class InnerSwapHost(
        private val computationScope: CoroutineScope
    ) : AssetExchange.SwapHost {

        override suspend fun quote(quoteArgs: ParentQuoterArgs): Balance {
            return quoteTrade(
                chainAssetIn = quoteArgs.chainAssetIn,
                chainAssetOut = quoteArgs.chainAssetOut,
                amount = quoteArgs.amount,
                swapDirection = quoteArgs.swapDirection,
                computationSharingScope = computationScope,
                logQuotes = false
            ).finalQuote()
        }

        override suspend fun extrinsicService(): ExtrinsicService {
            return extrinsicService(computationScope)
        }
    }

    private fun logFee(fee: SwapFee) {
        val route = fee.segments.joinToString(separator = "\n") { segment ->
            val allFees = buildList {
                add(segment.fee.submissionFee)
                addAll(segment.fee.postSubmissionFees.paidByAccount)
                addAll(segment.fee.postSubmissionFees.paidFromAmount)
            }

            allFees.joinToString { "${it.amount.formatPlanks(it.asset)} (${it.debugLabel})" }
        }

        Log.d("Swaps", "---- Fees -----")
        Log.d("Swaps", route)
        Log.d("Swaps", "---- End Fees -----")
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
                val amountIn: Balance
                val amountOut: Balance

                when(trade.direction) {
                    SwapDirection.SPECIFIED_IN -> {
                        amountIn = quotedSwapEdge.quotedAmount
                        amountOut = quotedSwapEdge.quote
                    }
                    SwapDirection.SPECIFIED_OUT -> {
                        amountIn = quotedSwapEdge.quote
                        amountOut = quotedSwapEdge.quotedAmount
                    }
                }

                if (index == 0) {
                    val assetIn = chainRegistry.asset(quotedSwapEdge.edge.from)
                    val initialAmount = amountIn.formatPlanks(assetIn)
                    append(initialAmount)
                }

                append(" --- " + quotedSwapEdge.edge.debugLabel() + " ---> ")

                val assetOut = chainRegistry.asset(quotedSwapEdge.edge.to)
                val outAmount = amountOut.formatPlanks(assetOut)

                append(outAmount)
            }
        }
    }

    private inner class ExchangeRegistry(
        private val singleChainExchanges: Map<ChainId, AssetExchange>,
        private val multiChainExchanges: List<AssetExchange>,
    ) {

        private val feePaymentRegistry = SwapFeePaymentRegistry()

        fun getFeePaymentRegistry(): FeePaymentProviderRegistry {
            return feePaymentRegistry
        }

        fun allExchanges(): List<AssetExchange> {
            return buildList {
                addAll(singleChainExchanges.values)
                addAll(multiChainExchanges)
            }
        }

        private inner class SwapFeePaymentRegistry : FeePaymentProviderRegistry {

            private val paymentRegistryOverrides = createFeePaymentOverrides()

            override suspend fun providerFor(chainId: ChainId): FeePaymentProvider {
                return paymentRegistryOverrides.find { it.chain.id == chainId }?.provider
                    ?: defaultFeePaymentProviderRegistry.providerFor(chainId)
            }

            private fun createFeePaymentOverrides(): List<FeePaymentProviderOverride> {
                return buildList {
                    singleChainExchanges.values.onEach { singleChainExchange ->
                        addAll(singleChainExchange.feePaymentOverrides())
                    }

                    multiChainExchanges.onEach { multiChainExchange ->
                        addAll(multiChainExchange.feePaymentOverrides())
                    }
                }
            }
        }
    }

    /**
     * Check that it is possible to pay fees in moving asset
     */
    private inner class CanPayFeeNodeVisitFilter(val computationScope: CoroutineScope) : EdgeVisitFilter<SwapGraphEdge> {

        private val feePaymentCapabilityCache: MutableMap<ChainId, Any> = mutableMapOf()

        private suspend fun getFeeCustomFeeCapability(chainId: ChainId): FastLookupCustomFeeCapability? {
            val fromCache = feePaymentCapabilityCache.getOrPut(chainId) {
                createFastLookupFeeCapability(chainId, computationScope).boxNullable()
            }

            return fromCache.unboxNullable()
        }

        private suspend fun createFastLookupFeeCapability(chainId: ChainId, computationScope: CoroutineScope): FastLookupCustomFeeCapability? {
            val feePaymentRegistry = exchangeRegistry(computationScope).getFeePaymentRegistry()
            return feePaymentRegistry.providerFor(chainId).fastLookupCustomFeeCapability()
        }

        override suspend fun shouldVisit(edge: SwapGraphEdge, pathPredecessor: SwapGraphEdge?): Boolean {
            // Utility payments and first path segments are always allowed
            if (edge.from.isUtility || pathPredecessor == null) return true

            // Edge might request us to ignore the default requirement based on its direct predecessor
            if (edge.shouldIgnoreFeeRequirementAfter(pathPredecessor)) return true

            val feeCapability = getFeeCustomFeeCapability(edge.from.chainId)

            return feeCapability != null && feeCapability.canPayFeeInNonUtilityToken(edge.from.assetId)
                && edge.canPayNonNativeFeesInIntermediatePosition()
        }
    }

    private object NULL

    fun <T> T.boxNullable(): Any = this ?: NULL

    @Suppress("UNCHECKED_CAST")
    fun <T> Any.unboxNullable(): T? = if (this == NULL) null else this as T
}

private typealias QuotedTrade = QuotedPath<SwapGraphEdge>

abstract class BaseSwapGraphEdge(
    val fromAsset: Chain.Asset,
    val toAsset: Chain.Asset
) : SwapGraphEdge {

    final override val from: FullChainAssetId = fromAsset.fullId

    final override val to: FullChainAssetId = toAsset.fullId
}
