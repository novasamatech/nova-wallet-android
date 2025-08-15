package io.novafoundation.nova.feature_swap_impl.domain.swap

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.SharedFlowCache
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.graph.findAllPossibleDestinations
import io.novafoundation.nova.common.utils.graph.hasOutcomingDirections
import io.novafoundation.nova.common.utils.graph.vertices
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.measureExecution
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionEstimate
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraph
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgressStep
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmissionResult
import io.novafoundation.nova.feature_swap_api.domain.model.UsdConverter
import io.novafoundation.nova.feature_swap_api.domain.model.amountToLeaveOnOriginToPayTxFees
import io.novafoundation.nova.feature_swap_api.domain.model.replaceAmountIn
import io.novafoundation.nova.feature_swap_api.domain.model.totalFeeEnsuringSubmissionAsset
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathFeeEstimator
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.firstSegmentQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.firstSegmentQuotedAmount
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.lastSegmentQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.lastSegmentQuotedAmount
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.weightBreakdown
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.BuildConfig
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.ParentQuoterArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.SharedSwapSubscriptions
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain.CrossChainTransferAssetExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromFiatOrZero
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.ext.isUtility
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.ext.utilityAssetOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAssetOrNull
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val ALL_DIRECTIONS_CACHE = "RealSwapService.ALL_DIRECTIONS"
private const val EXCHANGES_CACHE = "RealSwapService.EXCHANGES"
private const val EXTRINSIC_SERVICE_CACHE = "RealSwapService.ExtrinsicService"
private const val QUOTER_CACHE = "RealSwapService.QUOTER"
private const val NODE_VISIT_FILTER = "RealSwapService.NodeVisitFilter"
private const val SHARED_SUBSCRIPTIONS = "RealSwapService.SharedSubscriptions"

private val ADDITIONAL_ESTIMATE_BUFFER = 3.seconds

internal class RealSwapService(
    private val assetConversionFactory: AssetConversionExchangeFactory,
    private val hydraDxExchangeFactory: HydraDxExchangeFactory,
    private val crossChainTransferFactory: CrossChainTransferAssetExchangeFactory,
    private val computationalCache: ComputationalCache,
    private val chainRegistry: ChainRegistry,
    private val quoterFactory: PathQuoter.Factory,
    private val extrinsicServiceFactory: ExtrinsicService.Factory,
    private val defaultFeePaymentProviderRegistry: FeePaymentProviderRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val chainStateRepository: ChainStateRepository,
    private val signerProvider: SignerProvider,
    private val debug: Boolean = BuildConfig.DEBUG
) : SwapService {

    override suspend fun warmUpCommonChains(computationScope: CoroutineScope): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.Default) {
                warmUpChain(Chain.Geneses.HYDRA_DX, computationScope)
                warmUpChain(Chain.Geneses.POLKADOT_ASSET_HUB, computationScope)
            }
        }
    }

    private suspend fun warmUpChain(chainId: ChainId, computationScope: CoroutineScope) {
        nodeVisitFilter(computationScope).warmUpChain(chainId)
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
            val filter = nodeVisitFilter(computationScope)
            measureExecution("findAllPossibleDestinations") {
                it.findAllPossibleDestinations(asset.fullId, filter) - asset.fullId
            }
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

        val firstOperation = atomicOperations.first()

        return SwapFee(
            segments = fees,
            intermediateSegmentFeesInAssetIn = convertedFees,
            additionalMaxAmountDeduction = firstOperation.additionalMaxAmountDeduction(),
        ).also(::logFee)
    }

    override suspend fun swap(calculatedFee: SwapFee): Flow<SwapProgress> {
        val segments = calculatedFee.segments

        val initialCorrection: Result<SwapExecutionCorrection?> = Result.success(null)

        return flow {
            segments.withIndex().fold(initialCorrection) { prevStepCorrection, (index, segment) ->
                val (segmentFee, operation) = segment

                prevStepCorrection.flatMap { correction ->
                    val displayData = operation.constructDisplayData()
                    val step = SwapProgressStep(index, displayData, operation)

                    emit(SwapProgress.StepStarted(step))

                    val newAmountIn = if (correction != null) {
                        correction.actualReceivedAmount - segmentFee.amountToLeaveOnOriginToPayTxFees()
                    } else {
                        val amountIn = operation.estimatedSwapLimit.estimatedAmountIn()
                        amountIn + calculatedFee.additionalAmountForSwap.amount
                    }

                    // We cannot execute buy for segments after first one since we deal with actualReceivedAmount there
                    val shouldReplaceBuyWithSell = correction != null
                    val actualSwapLimit = operation.estimatedSwapLimit.replaceAmountIn(newAmountIn, shouldReplaceBuyWithSell)
                    val segmentSubmissionArgs = AtomicSwapOperationSubmissionArgs(actualSwapLimit)

                    Log.d("SwapSubmission", "$displayData with $actualSwapLimit")

                    operation.execute(segmentSubmissionArgs).onFailure {
                        Log.e("SwapSubmission", "Swap failed on stage '$displayData'", it)

                        emit(SwapProgress.Failure(it, attemptedStep = step))
                    }
                }
            }.onSuccess {
                emit(SwapProgress.Done)
            }
        }
    }

    override suspend fun submitFirstSwapStep(calculatedFee: SwapFee): Result<SwapSubmissionResult> {
        val (_, operation) = calculatedFee.segments.firstOrNull() ?: return Result.failure(IllegalStateException("No segments"))

        val amountIn = operation.estimatedSwapLimit.estimatedAmountIn() + calculatedFee.additionalAmountForSwap.amount
        val actualSwapLimit = operation.estimatedSwapLimit.replaceAmountIn(amountIn, false)

        val segmentSubmissionArgs = AtomicSwapOperationSubmissionArgs(actualSwapLimit)

        return operation.submit(segmentSubmissionArgs)
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

    private suspend fun Path<QuotedEdge<SwapGraphEdge>>.constructAtomicOperationPrototypes(): List<AtomicSwapOperationPrototype> {
        var currentSwapTx: AtomicSwapOperationPrototype? = null
        val finishedSwapTxs = mutableListOf<AtomicSwapOperationPrototype>()

        forEach { quotedEdge ->
            // Initial case - begin first operation
            if (currentSwapTx == null) {
                currentSwapTx = quotedEdge.edge.beginOperationPrototype()
                return@forEach
            }

            // Try to append segment to current swap tx
            val maybeAppendedCurrentTx = quotedEdge.edge.appendToOperationPrototype(currentSwapTx!!)

            currentSwapTx = if (maybeAppendedCurrentTx == null) {
                finishedSwapTxs.add(currentSwapTx!!)
                quotedEdge.edge.beginOperationPrototype()
            } else {
                maybeAppendedCurrentTx
            }
        }

        finishedSwapTxs.add(currentSwapTx!!)

        return finishedSwapTxs
    }

    private suspend fun SwapGraphEdge.identifySegmentCurrency(
        isFirstSegment: Boolean,
        firstSegmentFees: FeePaymentCurrency
    ): FeePaymentCurrency {
        return if (isFirstSegment) {
            firstSegmentFees
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

        val atomicOperationsEstimates = quotedTrade.estimateOperationsMaximumExecutionTime()

        return SwapQuote(
            amountIn = args.tokenIn.configuration.withAmount(amountIn),
            amountOut = args.tokenOut.configuration.withAmount(amountOut),
            priceImpact = args.calculatePriceImpact(amountIn, amountOut),
            quotedPath = quotedTrade,
            executionEstimate = SwapExecutionEstimate(atomicOperationsEstimates, ADDITIONAL_ESTIMATE_BUFFER),
            direction = args.swapDirection,
        )
    }

    private suspend fun QuotedTrade.estimateOperationsMaximumExecutionTime(): List<Duration> {
        return path.constructAtomicOperationPrototypes()
            .map { it.maximumExecutionTime() }
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

    override suspend fun isDeepSwapAllowed(): Boolean {
        val signer = signerProvider.rootSignerFor(accountRepository.getSelectedMetaAccount())
        return when (signer.callExecutionType()) {
            CallExecutionType.IMMEDIATE -> true
            CallExecutionType.DELAYED -> false
        }
    }

    private fun SwapQuoteArgs.calculatePriceImpact(amountIn: Balance, amountOut: Balance): Fraction {
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

    private fun calculatePriceImpact(fiatIn: BigDecimal, fiatOut: BigDecimal): Fraction {
        if (fiatIn.isZero || fiatOut.isZero) return Fraction.ZERO

        val priceImpact = (BigDecimal.ONE - fiatOut / fiatIn).atLeastZero()

        return priceImpact.fractions
    }

    private suspend fun directionsGraph(computationScope: CoroutineScope): Flow<SwapGraph> {
        return computationalCache.useSharedFlow(ALL_DIRECTIONS_CACHE, computationScope) {
            val exchangeRegistry = exchangeRegistry(computationScope)

            val directionsByExchange = exchangeRegistry.allExchanges().map { exchange ->
                flowOf { exchange.availableDirectSwapConnections() }
                    .catch {
                        emit(emptyList())

                        Log.e("RealSwapService", "Failed to fetch directions for exchange ${exchange::class.simpleName}", it)
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

    private suspend fun nodeVisitFilter(computationScope: CoroutineScope): NodeVisitFilter {
        return computationalCache.useCache(NODE_VISIT_FILTER, computationScope) {
            NodeVisitFilter(
                computationScope = this,
                chainsById = chainRegistry.chainsById(),
                selectedAccount = accountRepository.getSelectedMetaAccount()
            )
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
                crossChainTransferFactory.create(createInnerSwapHost(coroutineScope))
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
        val host = createInnerSwapHost(coroutineScope)

        return chainRegistry.enabledChainById().mapValues { (_, chain) ->
            createSingleExchange(chain, host)
        }
            .filterNotNull()
    }

    private suspend fun createSingleExchange(
        chain: Chain,
        host: AssetExchange.SwapHost
    ): AssetExchange? {
        val factory = when {
            chain.swap.assetConversionSupported() -> assetConversionFactory
            chain.swap.hydraDxSupported() -> hydraDxExchangeFactory
            else -> null
        }

        return factory?.create(chain, host)
    }

    private suspend fun createInnerSwapHost(computationScope: CoroutineScope): InnerSwapHost {
        val subscriptions = sharedSwapSubscriptions(computationScope)
        return InnerSwapHost(computationScope, subscriptions)
    }

    private suspend fun sharedSwapSubscriptions(computationScope: CoroutineScope): SharedSwapSubscriptions {
        return computationalCache.useCache(SHARED_SUBSCRIPTIONS, computationScope) {
            RealSharedSwapSubscriptions(computationScope)
        }
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
            val graphFlow = directionsGraph(computationScope)
            val filter = nodeVisitFilter(computationScope)

            quoterFactory.create(graphFlow, this, SwapPathFeeEstimator(), filter)
        }
    }

    private inner class SwapPathFeeEstimator : PathFeeEstimator<SwapGraphEdge> {

        override suspend fun roughlyEstimateFee(path: Path<QuotedEdge<SwapGraphEdge>>): PathRoughFeeEstimation {
            // USDT is used to determine usd to selected currency rate without making a separate request to price api
            val usdtOnAssetHub = chainRegistry.getUSDTOnAssetHub() ?: return PathRoughFeeEstimation.zero()

            val operationPrototypes = path.constructAtomicOperationPrototypes()

            val nativeAssetsSegments = operationPrototypes.allNativeAssets()
            val assetIn = chainRegistry.asset(path.first().edge.from)
            val assetOut = chainRegistry.asset(path.last().edge.to)

            val prices = getTokens(assetIn = assetIn, assetOut = assetOut, usdTiedAsset = usdtOnAssetHub, fees = nativeAssetsSegments)

            val totalFiat = operationPrototypes.estimateTotalFeeInFiat(prices, usdtOnAssetHub.fullId)

            return PathRoughFeeEstimation(
                inAssetIn = prices.fiatToPlanks(totalFiat, assetIn),
                inAssetOut = prices.fiatToPlanks(totalFiat, assetOut)
            )
        }

        private suspend fun ChainRegistry.getUSDTOnAssetHub(): Chain.Asset? {
            val assetHub = getChain(Chain.Geneses.POLKADOT_ASSET_HUB)
            return assetHub.assets.find { it.symbol.value == "USDT" }
        }

        private fun Map<FullChainAssetId, Token>.fiatToPlanks(fiat: BigDecimal, chainAsset: Chain.Asset): Balance {
            val token = get(chainAsset.fullId) ?: return Balance.ZERO

            return token.planksFromFiatOrZero(fiat)
        }

        private suspend fun getTokens(
            assetIn: Chain.Asset,
            assetOut: Chain.Asset,
            usdTiedAsset: Chain.Asset,
            fees: List<Chain.Asset>
        ): Map<FullChainAssetId, Token> {
            val allTokensToRequestPrices = buildList {
                addAll(fees)
                add(assetIn)
                add(usdTiedAsset)
                add(assetOut)
            }

            return tokenRepository.getTokens(allTokensToRequestPrices)
        }

        private suspend fun List<AtomicSwapOperationPrototype>.allNativeAssets(): List<Chain.Asset> {
            return map {
                val chain = chainRegistry.getChain(it.fromChain)
                chain.utilityAsset
            }
        }

        private suspend fun List<AtomicSwapOperationPrototype>.estimateTotalFeeInFiat(
            prices: Map<FullChainAssetId, Token>,
            usdTiedAsset: FullChainAssetId
        ): BigDecimal {
            return sumOf {
                val nativeAssetId = FullChainAssetId.utilityAssetOf(it.fromChain)
                val token = prices[nativeAssetId] ?: return@sumOf BigDecimal.ZERO

                val usdConverter = PriceBasedUsdConverter(prices, nativeAssetId, usdTiedAsset)

                val roughFee = it.roughlyEstimateNativeFee(usdConverter)
                token.amountToFiat(roughFee)
            }
        }

        private inner class PriceBasedUsdConverter(
            private val prices: Map<FullChainAssetId, Token>,
            private val nativeAsset: FullChainAssetId,
            private val usdTiedAsset: FullChainAssetId,
        ) : UsdConverter {

            val currencyToUsdRate = determineCurrencyToUsdRate()

            override suspend fun nativeAssetEquivalentOf(usdAmount: Double): BigDecimal {
                val priceInCurrency = prices[nativeAsset]?.coinRate?.rate ?: return BigDecimal.ZERO
                val priceInUsd = priceInCurrency * currencyToUsdRate
                return usdAmount.toBigDecimal() / priceInUsd
            }

            private fun determineCurrencyToUsdRate(): BigDecimal {
                val usdTiedAssetPrice = prices[usdTiedAsset] ?: return BigDecimal.ZERO
                val rate = usdTiedAssetPrice.coinRate?.rate.orZero()
                if (rate.isZero) return BigDecimal.ZERO

                return BigDecimal.ONE.divide(rate, MathContext.DECIMAL64)
            }
        }
    }

    private inner class InnerSwapHost(
        override val scope: CoroutineScope,
        override val sharedSubscriptions: SharedSwapSubscriptions
    ) : AssetExchange.SwapHost {

        override suspend fun quote(quoteArgs: ParentQuoterArgs): Balance {
            return quoteTrade(
                chainAssetIn = quoteArgs.chainAssetIn,
                chainAssetOut = quoteArgs.chainAssetOut,
                amount = quoteArgs.amount,
                swapDirection = quoteArgs.swapDirection,
                computationSharingScope = scope,
                logQuotes = false
            ).finalQuote()
        }

        override suspend fun extrinsicService(): ExtrinsicService {
            return extrinsicService(scope)
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
            val weightBreakdown = trade.weightBreakdown()

            trade.path.zip(weightBreakdown.individualWeights).onEachIndexed { index, (quotedSwapEdge, weight) ->
                val amountIn: Balance
                val amountOut: Balance

                when (trade.direction) {
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

                    if (trade.direction == SwapDirection.SPECIFIED_OUT) {
                        val roughFeesInAssetIn = trade.roughFeeEstimation.inAssetIn
                        val roughFeesInAssetInAmount = roughFeesInAssetIn.formatPlanks(assetIn)

                        append(" (+$roughFeesInAssetInAmount fees) ")
                    }
                }

                append(" --- ${quotedSwapEdge.edge.debugLabel()} (w: ${weight})---> ")

                val assetOut = chainRegistry.asset(quotedSwapEdge.edge.to)
                val outAmount = amountOut.formatPlanks(assetOut)

                append(outAmount)

                if (index == trade.path.size - 1) {
                    if (trade.direction == SwapDirection.SPECIFIED_IN) {
                        val roughFeesInAssetOut = trade.roughFeeEstimation.inAssetOut
                        val roughFeesInAssetOutAmount = roughFeesInAssetOut.formatPlanks(assetOut)

                        append(" (-$roughFeesInAssetOutAmount fees, w: ${weightBreakdown.total})")
                    }
                }
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
    private inner class NodeVisitFilter(
        val computationScope: CoroutineScope,
        val chainsById: ChainsById,
        val selectedAccount: MetaAccount,
    ) : EdgeVisitFilter<SwapGraphEdge> {

        private val feePaymentCapabilityCache: MutableMap<ChainId, Any> = mutableMapOf()
        private val callExecutionType = lazyAsync {
            signerProvider.rootSignerFor(selectedAccount)
                .callExecutionType()
        }

        suspend fun warmUpChain(chainId: ChainId) {
            getFeeCustomFeeCapability(chainId)
        }

        override suspend fun shouldVisit(edge: SwapGraphEdge, pathPredecessor: SwapGraphEdge?): Boolean {
            val chainAndAssetOut = chainsById.chainWithAssetOrNull(edge.to) ?: return false

            // User should have account on destination
            if (!selectedAccount.hasAccountIn(chainAndAssetOut.chain)) return false

            // First path segments don't have any extra restrictions
            if (pathPredecessor == null) return true

            //
            if (callExecutionType.get() == CallExecutionType.DELAYED) return false

            // We don't (yet) handle edges that doesn't allow to transfer whole account balance out
            if (!edge.canTransferOutWholeAccountBalance()) return false

            // Destination asset must be sufficient
            if (!isSufficient(chainAndAssetOut)) return false

            val chainAndAssetIn = chainsById.chainWithAssetOrNull(edge.from) ?: return false

            // Since we allow insufficient asset out in paths with length 1, we want to reject paths with length > 1
            // by checking sufficiency of assetIn (which was assetOut in the previous segment)
            if (!isSufficient(chainAndAssetIn)) return false

            // Besides checks above, utility assets don't have any other restrictions
            if (edge.from.isUtility) return true

            // Edge might request us to ignore the default requirement based on its direct predecessor
            if (edge.predecessorHandlesFees(pathPredecessor)) return true

            val feeCapability = getFeeCustomFeeCapability(edge.from.chainId)

            return feeCapability != null && feeCapability.canPayFeeInNonUtilityToken(edge.from.assetId) &&
                edge.canPayNonNativeFeesInIntermediatePosition()
        }

        private fun isSufficient(chainAndAsset: ChainWithAsset): Boolean {
            val balance = assetSourceRegistry.sourceFor(chainAndAsset.asset).balance
            return balance.isSelfSufficient(chainAndAsset.asset)
        }

        private suspend fun getFeeCustomFeeCapability(chainId: ChainId): FastLookupCustomFeeCapability? {
            val fromCache = feePaymentCapabilityCache.getOrPut(chainId) {
                createFastLookupFeeCapability(chainId, computationScope).boxNullable()
            }

            return fromCache.unboxNullable()
        }

        private suspend fun createFastLookupFeeCapability(chainId: ChainId, computationScope: CoroutineScope): FastLookupCustomFeeCapability? {
            val feePaymentRegistry = exchangeRegistry(computationScope).getFeePaymentRegistry()
            return feePaymentRegistry.providerFor(chainId).fastLookupCustomFeeCapability()
                .onFailure { Log.e("Swap", "Failed to construct fast custom fee lookup for chain $chainId", it) }
                .getOrNull()
        }
    }

    private inner class RealSharedSwapSubscriptions(
        private val coroutineScope: CoroutineScope,
    ) : SharedSwapSubscriptions, CoroutineScope by coroutineScope {

        private val blockNumberCache = SharedFlowCache<ChainId, BlockNumber>(coroutineScope) { chainId ->
            chainStateRepository.currentRemoteBlockNumberFlow(chainId)
        }

        override suspend fun blockNumber(chainId: ChainId): Flow<BlockNumber> {
            return blockNumberCache.getOrCompute(chainId)
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
