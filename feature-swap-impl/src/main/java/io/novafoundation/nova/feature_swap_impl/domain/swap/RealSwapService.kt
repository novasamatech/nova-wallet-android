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
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requestedAccountPaysFees
import io.novafoundation.nova.feature_swap_api.domain.model.QuotableEdge
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraph
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapPath
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_api.domain.model.SwapTransaction
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.BuildConfig
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
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

    override suspend fun estimateFee(quote: SwapQuote): SwapFee {


        val swapFee: SwapFee? = quote.path.fold<SwapGraphEdge, SwapFee?>(null) { acc, edge ->
            val segmentFee = edge.
        }

        val computationScope = CoroutineScope(coroutineContext)
        val exchange = exchanges(computationScope).getValue(args.assetIn.chainId)

        val assetExchangeFee = exchange.estimateFee(args)

        return SwapFee(networkFee = assetExchangeFee.networkFee, minimumBalanceBuyIn = assetExchangeFee.minimumBalanceBuyIn)
    }

    private fun QuotedPath.toTransactionList(): List<SwapTransaction> {
        val transactions = mutableListOf<SwapTransaction>()
        var currentTransaction: SwapTransaction? = null

        path.forEach { edge ->
            if (currentTransaction == null) {
                currentTransaction = edge.beginTransaction()
            }
        }
    }

    private suspend fun quoteInternal(
        args: SwapQuoteArgs,
        computationSharingScope: CoroutineScope
    ): SwapQuote {
        val from = args.tokenIn.configuration.fullId
        val to = args.tokenOut.configuration.fullId

        val paths = pathsFromCacheOrCompute(from, to, computationSharingScope) {
            val graph = directionsGraph(computationSharingScope).first()

            graph.findDijkstraPathsBetween(from, to, limit = PATHS_LIMIT)
        }

        val quotedPaths = paths.mapNotNull { path -> quotePath(path, args.amount, args.swapDirection) }
        if (paths.isEmpty()) {
            throw SwapQuoteException.NotEnoughLiquidity
        }

        val bestPathQuote = quotedPaths.max()

        val (amountIn, amountOut) = args.inAndOutAmounts(bestPathQuote)

        return SwapQuote(
            amountIn = args.tokenIn.configuration.withAmount(amountIn),
            amountOut = args.tokenOut.configuration.withAmount(amountOut),
            direction = args.swapDirection,
            priceImpact = args.calculatePriceImpact(amountIn, amountOut),
            path = bestPathQuote.path
        )
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        val computationScope = CoroutineScope(coroutineContext)

        return runCatching { exchanges(computationScope).getValue(args.assetIn.chainId) }
            .flatMap { exchange -> exchange.swap(args) }
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

    private fun SwapQuoteArgs.inAndOutAmounts(quote: QuotedPath): Pair<Balance, Balance> {
        return when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> amount to quote.quote
            SwapDirection.SPECIFIED_OUT -> quote.quote to amount
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

        return factory?.create(chain, computationScope)
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
    ): QuotedPath? {
        val quote = when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> quotePathSell(path, amount)
            SwapDirection.SPECIFIED_OUT -> quotePathBuy(path, amount)
        } ?: return null

        return QuotedPath(swapDirection, quote, path)
    }

    private suspend fun quotePathBuy(path: Path<SwapGraphEdge>, amount: Balance): Balance? {
        return runCatching {
            path.foldRight(amount) { segment, currentAmount ->
                segment.quote(currentAmount, SwapDirection.SPECIFIED_OUT)
            }
        }.getOrNull()
    }

    private suspend fun quotePathSell(path: Path<SwapGraphEdge>, amount: Balance): Balance? {
        return runCatching {
            path.fold(amount) { currentAmount, segment ->
                segment.quote(currentAmount, SwapDirection.SPECIFIED_IN)
            }
        }.getOrNull()
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

private class QuotedPath(
    val direction: SwapDirection,

    val quote: Balance,

    val path: SwapPath
) : Comparable<QuotedPath> {

    override fun compareTo(other: QuotedPath): Int {
        return when (direction) {
            // When we want to sell a token, the bigger the quote - the better
            SwapDirection.SPECIFIED_IN -> (quote - other.quote).signum()
            // When we want to buy a token, the smaller the quote - the better
            SwapDirection.SPECIFIED_OUT -> (other.quote - quote).signum()
        }
    }
}
