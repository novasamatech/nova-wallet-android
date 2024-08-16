package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import android.util.Log
import io.novafoundation.nova.common.utils.firstById
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.graph.findDijkstraPathsBetween
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.multiTransactionPayment
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.BuildConfig
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.data.network.toChainAssetOrThrow
import io.novafoundation.nova.feature_swap_core.domain.model.QuotePath
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_core.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap.StableConversionSourceFactory
import io.novafoundation.nova.feature_swap_core.data.network.isSystemAsset
import io.novafoundation.nova.feature_swap_core.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

private const val PATHS_LIMIT = 4

class RealHydraDxAssetConversionFactory(
    private val remoteStorageSource: StorageDataSource,
    private val conversionSourceFactories: Iterable<HydraDxConversionSource.Factory>,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxAssetConversionFactory {

    override fun create(chain: Chain): HydraDXAssetConversion {
        return RealHydraDxAssetConversion(
            chain,
            remoteStorageSource,
            conversionSourceFactories,
            hydraDxAssetIdConverter
        )
    }
}

class RealHydraDxAssetConversion(
    private val chain: Chain,
    private val remoteStorageSource: StorageDataSource,
    private val swapSourceFactories: Iterable<HydraDxConversionSource.Factory>,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val debug: Boolean = BuildConfig.DEBUG
) : HydraDXAssetConversion {

    private val conversionSources: List<HydraDxConversionSource> = createSources()

    override suspend fun sync() {
        conversionSources.forEachAsync { it.sync() }
    }

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
        val onChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(chainAsset)

        if (hydraDxAssetIdConverter.isSystemAsset(onChainId)) return true

        val fallbackPrice = remoteStorageSource.query(chain.id) {
            metadata.multiTransactionPayment.acceptedCurrencies.query(onChainId)
        }

        return fallbackPrice != null
    }

    override suspend fun availableSwapDirections(): Graph<FullChainAssetId, HydraDxSwapEdge> {
        val allDirectDirections = conversionSources.mapAsync { source ->
            source.availableSwapDirections().mapValues { (from, directions) ->
                directions.map { direction -> HydraDxSwapEdge(from, source.identifier, direction) }
            }
        }

        return Graph.create(allDirectDirections)
    }

    override suspend fun getPaths(graph: Graph<FullChainAssetId, HydraDxSwapEdge>, args: AssetExchangeQuoteArgs): List<Path<HydraDxSwapEdge>> {
        val from = args.chainAssetIn.fullId
        val to = args.chainAssetOut.fullId

        return graph.findDijkstraPathsBetween(from, to, limit = PATHS_LIMIT)
    }

    override suspend fun quote(paths: List<Path<HydraDxSwapEdge>>, args: AssetExchangeQuoteArgs): AssetExchangeQuote {
        val quotedPaths = paths.mapNotNull { path -> quotePath(path, args.amount, args.swapDirection) }
        if (paths.isEmpty()) {
            throw SwapQuoteException.NotEnoughLiquidity
        }

        if (debug) {
            logQuotes(args, quotedPaths)
        }

        return quotedPaths.max()
    }

    override suspend fun runSubscriptions(userAccountId: AccountId, subscriptionBuilder: SharedRequestsBuilder): Flow<Unit> {
        return conversionSources.map {
            it.runSubscriptions(userAccountId, subscriptionBuilder)
        }.mergeIfMultiple()
    }

    private suspend fun quotePath(
        path: Path<HydraDxSwapEdge>,
        amount: BigInteger,
        swapDirection: SwapDirection
    ): AssetExchangeQuote? {
        val quote = when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> quotePathSell(path, amount)
            SwapDirection.SPECIFIED_OUT -> quotePathBuy(path, amount)
        } ?: return null

        return AssetExchangeQuote(swapDirection, quote, path.toQuotePath())
    }

    private suspend fun quotePathBuy(path: Path<HydraDxSwapEdge>, amount: BigInteger): BigInteger? {
        return runCatching {
            path.foldRight(amount) { segment, currentAmount ->
                val args = HydraDxConversionSourceQuoteArgs(
                    chainAssetIn = chain.assetsById.getValue(segment.from.assetId),
                    chainAssetOut = chain.assetsById.getValue(segment.to.assetId),
                    amount = currentAmount,
                    swapDirection = SwapDirection.SPECIFIED_OUT,
                    params = segment.direction.params
                )

                segment.swapSource().quote(args)
            }
        }.getOrNull()
    }

    private suspend fun quotePathSell(path: Path<HydraDxSwapEdge>, amount: BigInteger): BigInteger? {
        return runCatching {
            path.fold(amount) { currentAmount, segment ->
                val args = HydraDxConversionSourceQuoteArgs(
                    chainAssetIn = chain.assetsById.getValue(segment.from.assetId),
                    chainAssetOut = chain.assetsById.getValue(segment.to.assetId),
                    amount = currentAmount,
                    swapDirection = SwapDirection.SPECIFIED_IN,
                    params = segment.direction.params
                )

                segment.swapSource().quote(args)
            }
        }.getOrNull()
    }

    private fun createSources(): List<HydraDxConversionSource> {
        return swapSourceFactories.map { it.create(chain) }
    }

    private fun HydraDxSwapEdge.swapSource(): HydraDxConversionSource {
        return conversionSources.firstById(sourceId)
    }

    private suspend fun logQuotes(args: AssetExchangeQuoteArgs, quotes: List<AssetExchangeQuote>) {
        val allCandidates = quotes.sortedDescending().map {
            val formattedIn = args.amount.toBigDecimal(scale = args.chainAssetIn.precision.value).toString() + " " + args.chainAssetIn.symbol
            val formattedOut = it.quote.toBigDecimal(scale = args.chainAssetOut.precision.value).toString() + " " + args.chainAssetOut.symbol
            val formattedPath = formatPath(it.path)

            "$formattedIn to $formattedOut via $formattedPath"
        }.joinToString(separator = "\n")

        Log.d("RealSwapService", "-------- New quote ----------")
        Log.d("RealSwapService", allCandidates)
        Log.d("RealSwapService", "-------- Done quote ----------\n\n\n")
    }

    private suspend fun formatPath(path: QuotePath): String {
        val assets = chain.assetsById

        return buildString {
            val firstSegment = path.segments.first()

            append(assets.getValue(firstSegment.from.assetId).symbol)

            append("  -- ${formatSource(firstSegment)} -->  ")

            append(assets.getValue(firstSegment.to.assetId).symbol)

            path.segments.subList(1, path.segments.size).onEach { segment ->
                append("  -- ${formatSource(segment)} -->  ")

                append(assets.getValue(segment.to.assetId).symbol)
            }
        }
    }

    private suspend fun formatSource(segment: QuotePath.Segment): String {
        return buildString {
            append(segment.sourceId)

            if (segment.sourceId == StableConversionSourceFactory.ID) {
                val onChainId = segment.sourceParams.getValue("PoolId").toBigInteger()
                val chainAsset = hydraDxAssetIdConverter.toChainAssetOrThrow(chain, onChainId)
                append("[${chainAsset.symbol}]")
            }
        }
    }
}
