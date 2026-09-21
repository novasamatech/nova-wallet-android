package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights.Hydra.weightAppendingToPath
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.model.AavePool
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.model.AavePools
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.matchId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.metadata.method
import io.novasama.substrate_sdk_android.runtime.metadata.runtimeApi
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.math.BigInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@FeatureScope
class AaveSwapQuotingSourceFactory @Inject constructor(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val reservesDiscovery: AaveReservesDiscovery,
) : HydraDxQuotingSource.Factory<AavePoolQuotingSource> {

    companion object {

        const val ID = "Aave"
    }

    override fun create(chain: Chain, host: SwapQuoting.QuotingHost): AavePoolQuotingSource {
        return RealAaveSwapQuotingSource(
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            reservesDiscovery = reservesDiscovery,
            chain = chain,
            host = host
        )
    }
}

private class RealAaveSwapQuotingSource(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val reservesDiscovery: AaveReservesDiscovery,
    private val chain: Chain,
    private val host: SwapQuoting.QuotingHost,
) : AavePoolQuotingSource {

    override val identifier: String = AaveSwapQuotingSourceFactory.ID

    private val initialState: MutableSharedFlow<AaveInitialState> = singleReplaySharedFlow()

    private val aavePools: MutableSharedFlow<AavePools> = singleReplaySharedFlow()

    private val individualPoolCallsSemaphore = Semaphore(INDIVIDUAL_POOL_CALLS_CONCURRENCY)

    override suspend fun sync() {
        val pairs = getPairs()
        initialState.emit(
            AaveInitialState(
                pairs = pairs,
                poolInfo = pairs.matchIdsWithLocal()
            )
        )
    }

    override suspend fun availableSwapDirections(): Collection<AavePoolQuotingSource.Edge> {
        val poolInitialInfo = initialState.first().poolInfo

        return poolInitialInfo.allPossibleDirections()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> = coroutineScope {
        aavePools.resetReplayCache()
        val pairs = initialState.first().pairs

        host.sharedSubscriptions.blockNumber(chain.id)
            .conflate()
            .transform {
                val fetchResult = runCatching { getPools(pairs) }
                    .onFailure { Log.w(LOG_TAG, "Failed to refresh Aave pools", it) }
                    .getOrNull()

                if (fetchResult == null) {
                    // Keep the previous complete snapshot. If this was the first load, quoting waits instead of
                    // silently using an incomplete Aave graph and potentially selecting a bad route.
                    delay(FAILED_POOLS_REFRESH_RETRY_INTERVAL)
                    return@transform
                }

                aavePools.emit(fetchResult.pools)
                emit(Unit)

                if (fetchResult.source == AavePoolsSource.INDIVIDUAL_CALLS) {
                    // The runtime aggregate call is currently broken. Avoid starting dozens of RPC calls every block;
                    // conflate keeps the latest block so refresh resumes from current state after this interval.
                    delay(INDIVIDUAL_POOLS_REFRESH_INTERVAL)
                }
            }
    }

    private suspend fun getPairs(): List<AavePoolPair> {
        val fromRuntime = runCatching {
            multiChainRuntimeCallsApi.forChain(chain.id).call(
                section = "AaveTradeExecutor",
                method = "pairs",
                arguments = emptyMap(),
                returnBinding = ::bindPairs
            )
        }.onFailure { Log.w(LOG_TAG, "Failed to get aave pairs from the runtime", it) }
            .getOrDefault(emptyList())

        if (fromRuntime.isNotEmpty()) return fromRuntime

        // The runtime enumerates reserves within a fixed gas limit and silently returns nothing once the
        // reserve list outgrows it. Without these pairs DOT and other Aave-backed assets lose their route into
        // the deep pools and get quoted through thin ones, so read the reserves from the Aave pool instead.
        Log.w(LOG_TAG, "Runtime returned no aave pairs, discovering them from the Aave pool contract")

        return runCatching { reservesDiscovery.discoverPairs(chain.id) }
            .onSuccess { Log.d(LOG_TAG, "Discovered ${it.size} aave pairs from the Aave pool contract") }
            .onFailure { Log.e(LOG_TAG, "Failed to discover aave pairs, Aave routes are unavailable", it) }
            .getOrDefault(emptyList())
    }

    private suspend fun getPools(pairs: List<AavePoolPair>): AavePoolsFetchResult {
        val runtimeCalls = multiChainRuntimeCallsApi.forChain(chain.id)

        val aggregatePools = runCatching {
            runtimeCalls.call(
                section = "AaveTradeExecutor",
                method = "pools",
                arguments = emptyMap(),
                returnBinding = ::bindPools
            )
        }.onFailure { Log.w(LOG_TAG, "Failed to get Aave pools from the runtime", it) }
            .getOrNull()

        if (aggregatePools != null && aggregatePools.containsAll(pairs)) {
            return AavePoolsFetchResult(aggregatePools, AavePoolsSource.AGGREGATE_CALL)
        }

        // The aggregate call shares the broken reserve enumeration with pairs(). Individual calls do not.
        val individualPools = AavePools(getPoolsOneByOne(runtimeCalls, pairs))
        return AavePoolsFetchResult(individualPools, AavePoolsSource.INDIVIDUAL_CALLS)
    }

    private suspend fun getPoolsOneByOne(runtimeCalls: RuntimeCallsApi, pairs: List<AavePoolPair>): List<AavePool> = coroutineScope {
        val inputNames = runtimeCalls.runtime.metadata.runtimeApi("AaveTradeExecutor").method("pool").inputs.map { it.name }

        pairs.map { pair ->
            async {
                individualPoolCallsSemaphore.withPermit {
                    runtimeCalls.call(
                        section = "AaveTradeExecutor",
                        method = "pool",
                        arguments = inputNames.zip(listOf(pair.firstAsset, pair.secondAsset)).toMap(),
                        returnBinding = ::bindPool
                    )
                }
            }
        }.awaitAll()
    }

    private fun AavePools.containsAll(pairs: List<AavePoolPair>): Boolean {
        val availablePairs = pools.mapTo(mutableSetOf()) { AavePoolPair(it.reserve, it.atoken) }
        return availablePairs.containsAll(pairs)
    }

    private suspend fun List<AavePoolPair>.matchIdsWithLocal(): List<AavePoolInitialInfo> {
        val allOnChainIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return mapNotNull { poolInfo ->
            AavePoolInitialInfo(
                firstAsset = allOnChainIds.matchId(poolInfo.firstAsset) ?: return@mapNotNull null,
                secondAsset = allOnChainIds.matchId(poolInfo.secondAsset) ?: return@mapNotNull null,
            )
        }
    }

    private fun Collection<AavePoolInitialInfo>.allPossibleDirections(): Collection<RealXYKSwapQuotingEdge> {
        return buildList {
            this@allPossibleDirections.forEach { poolInfo ->
                add(RealXYKSwapQuotingEdge(fromAsset = poolInfo.firstAsset, toAsset = poolInfo.secondAsset))
                add(RealXYKSwapQuotingEdge(fromAsset = poolInfo.secondAsset, toAsset = poolInfo.firstAsset))
            }
        }
    }

    inner class RealXYKSwapQuotingEdge(
        override val fromAsset: RemoteAndLocalId,
        override val toAsset: RemoteAndLocalId,
    ) : AavePoolQuotingSource.Edge {

        override val from: FullChainAssetId = fromAsset.second

        override val to: FullChainAssetId = toAsset.second

        override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>): Int {
            return weightAppendingToPath(path, Weights.Hydra.AAVE)
        }

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            val allPools = aavePools.first()

            return allPools.quote(fromAsset.first, toAsset.first, amount, direction)
                ?: throw SwapQuoteException.NotEnoughLiquidity
        }
    }

    private fun bindPairs(decoded: Any?): List<AavePoolPair> {
        return bindList(decoded) { item ->
            val (first, second) = item.castToList()
            AavePoolPair(bindNumber(first), bindNumber(second))
        }
    }

    private fun bindPools(decoded: Any?): AavePools {
        val pools = bindList(decoded, ::bindPool)
        return AavePools(pools)
    }

    private fun bindPool(decoded: Any?): AavePool {
        val asStruct = decoded.castToStruct()

        return AavePool(
            reserve = bindNumber(asStruct["reserve"]),
            atoken = bindNumber(asStruct["atoken"]),
            liqudityIn = bindNumber(asStruct["liqudityIn"]),
            liquidityOut = bindNumber(asStruct["liqudityOut"])
        )
    }
}

private val INDIVIDUAL_POOLS_REFRESH_INTERVAL = 20.seconds
private val FAILED_POOLS_REFRESH_RETRY_INTERVAL = 5.seconds
private const val INDIVIDUAL_POOL_CALLS_CONCURRENCY = 6

private data class AaveInitialState(
    val pairs: List<AavePoolPair>,
    val poolInfo: List<AavePoolInitialInfo>
)

private data class AavePoolsFetchResult(
    val pools: AavePools,
    val source: AavePoolsSource
)

private enum class AavePoolsSource {
    AGGREGATE_CALL,
    INDIVIDUAL_CALLS
}

private class AavePoolInitialInfo(
    val firstAsset: RemoteAndLocalId,
    val secondAsset: RemoteAndLocalId
)
