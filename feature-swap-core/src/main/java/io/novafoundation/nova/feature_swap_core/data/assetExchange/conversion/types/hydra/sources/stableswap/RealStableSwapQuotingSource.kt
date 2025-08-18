package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.toMultiSubscription
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.HydrationAssetMetadataMap
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.HydrationBalanceFetcher
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.HydrationBalanceFetcherFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.assetRegistry
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.assets
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalIdOptional
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.flatten
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.omniPoolAccountId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.StablePool
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.StablePoolAsset
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.StableSwapPoolInfo
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.StalbeSwapPoolPegInfo
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model.quote
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.encrypt.json.asLittleEndianBytes
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Named
import io.novafoundation.nova.common.utils.combine as combine6

@FeatureScope
class StableSwapQuotingSourceFactory @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydrationBalanceFetcherFactory: HydrationBalanceFetcherFactory,
    private val gson: Gson,
) : HydraDxQuotingSource.Factory<StableSwapQuotingSource> {

    companion object {

        const val ID = "StableSwap"
    }

    override fun create(chain: Chain, host: SwapQuoting.QuotingHost): StableSwapQuotingSource {
        return RealStableSwapQuotingSource(
            remoteStorageSource = remoteStorageSource,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydrationBalanceFetcher = hydrationBalanceFetcherFactory.create(host),
            chain = chain,
            gson = gson,
            host = host
        )
    }
}

private class RealStableSwapQuotingSource(
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydrationBalanceFetcher: HydrationBalanceFetcher,
    override val chain: Chain,
    private val gson: Gson,
    private val host: SwapQuoting.QuotingHost,
) : StableSwapQuotingSource {

    override val identifier: String = StableSwapQuotingSourceFactory.ID

    private val initialPoolsInfo: MutableSharedFlow<Collection<PoolInitialInfo>> = singleReplaySharedFlow()

    private val stablePools: MutableSharedFlow<List<StablePool>> = singleReplaySharedFlow()

    override suspend fun sync() {
        val pools = getPools()

        val poolInitialInfo = pools.matchIdsWithLocal()
        initialPoolsInfo.emit(poolInitialInfo)
    }

    override suspend fun availableSwapDirections(): Collection<StableSwapQuotingSource.Edge> {
        val poolInitialInfo = initialPoolsInfo.first()

        return poolInitialInfo.allPossibleDirections()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> = coroutineScope {
        stablePools.resetReplayCache()

        val initialPoolsInfo = initialPoolsInfo.first()

        val poolInfoSubscriptions = initialPoolsInfo.map { poolInfo ->
            remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                runtime.metadata.stableSwap.pools.observe(poolInfo.sharedAsset.first).map {
                    poolInfo.sharedAsset.first to it
                }
            }
        }.toMultiSubscription(initialPoolsInfo.size)

        val omniPoolAccountId = omniPoolAccountId()

        val allAssetIds = initialPoolsInfo.collectAllAssetIds()
        val assetsMetadataMap = fetchAssetMetadataMap(allAssetIds)

        val poolSharedAssetBalanceSubscriptions = initialPoolsInfo.map { poolInfo ->
            val sharedAssetRemoteId = poolInfo.sharedAsset.first

            subscribeTransferableBalance(subscriptionBuilder, omniPoolAccountId, sharedAssetRemoteId, assetsMetadataMap).map {
                sharedAssetRemoteId to it
            }
        }.toMultiSubscription(initialPoolsInfo.size)

        val totalPooledAssets = initialPoolsInfo.sumOf { it.poolAssets.size }

        val poolParticipatingAssetsBalanceSubscription = initialPoolsInfo.flatMap { poolInfo ->
            val poolAccountId = stableSwapPoolAccountId(poolInfo.sharedAsset.first)

            poolInfo.poolAssets.map { poolAsset ->
                subscribeTransferableBalance(subscriptionBuilder, poolAccountId, poolAsset.first, assetsMetadataMap).map {
                    val key = poolInfo.sharedAsset.first to poolAsset.first
                    key to it
                }
            }
        }.toMultiSubscription(totalPooledAssets)

        val totalIssuanceSubscriptions = initialPoolsInfo.map { poolInfo ->
            remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                runtime.metadata.hydraTokens.totalIssuance.observe(poolInfo.sharedAsset.first).map {
                    poolInfo.sharedAsset.first to it.orZero()
                }
            }
        }.toMultiSubscription(initialPoolsInfo.size)

        val pegsSubscriptions = initialPoolsInfo.map { poolInfo ->
            remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                val poolId = poolInfo.sharedAsset.first
                runtime.metadata.stableSwap.poolPegs.observe(poolId).map {
                    poolId to it
                }
            }
        }.toMultiSubscription(initialPoolsInfo.size)

        combine6(
            poolInfoSubscriptions,
            poolSharedAssetBalanceSubscriptions,
            poolParticipatingAssetsBalanceSubscription,
            totalIssuanceSubscriptions,
            host.sharedSubscriptions.blockNumber(chain.id),
            pegsSubscriptions
        ) { poolInfos, poolSharedAssetBalances, poolParticipatingAssetBalances, totalIssuances, currentBlock, pegs ->
            createStableSwapPool(poolInfos, poolSharedAssetBalances, poolParticipatingAssetBalances, totalIssuances, currentBlock, assetsMetadataMap, pegs)
        }
            .onEach(stablePools::emit)
            .map { }
    }

    private suspend fun subscribeTransferableBalance(
        subscriptionBuilder: SharedRequestsBuilder,
        account: AccountId,
        assetId: HydraDxAssetId,
        assetMetadataMap: HydrationAssetMetadataMap,
    ): Flow<BigInteger> {
        // In case token type was not possible to resolve - just return zero
        val tokenType = assetMetadataMap.getAssetType(assetId) ?: return flowOf(BalanceOf.ZERO)
        return hydrationBalanceFetcher.subscribeToTransferableBalance(chain.id, tokenType, account, subscriptionBuilder)
    }

    private fun createStableSwapPool(
        poolInfos: Map<HydraDxAssetId, StableSwapPoolInfo?>,
        poolSharedAssetBalances: Map<HydraDxAssetId, BigInteger>,
        poolParticipatingAssetBalances: Map<Pair<HydraDxAssetId, HydraDxAssetId>, BigInteger>,
        totalIssuances: Map<HydraDxAssetId, BigInteger>,
        currentBlock: BlockNumber,
        assetMetadataMap: HydrationAssetMetadataMap,
        pegs: Map<HydraDxAssetId, StalbeSwapPoolPegInfo?>
    ): List<StablePool> {
        return poolInfos.mapNotNull outer@{ (poolId, poolInfo) ->
            if (poolInfo == null) return@outer null

            val sharedAssetBalance = poolSharedAssetBalances[poolId].orZero()
            val sharedChainAssetPrecision = assetMetadataMap.getDecimals(poolId) ?: return@outer null
            val sharedAsset = StablePoolAsset(sharedAssetBalance, poolId, sharedChainAssetPrecision)
            val sharedAssetIssuance = totalIssuances[poolId].orZero()

            val pooledAssets = poolInfo.assets.mapNotNull { pooledAssetId ->
                val pooledAssetBalance = poolParticipatingAssetBalances[poolId to pooledAssetId].orZero()
                val decimals = assetMetadataMap.getDecimals(pooledAssetId) ?: return@mapNotNull null

                StablePoolAsset(pooledAssetBalance, pooledAssetId, decimals)
            }

            StablePool(
                sharedAsset = sharedAsset,
                assets = pooledAssets,
                initialAmplification = poolInfo.initialAmplification,
                finalAmplification = poolInfo.finalAmplification,
                initialBlock = poolInfo.initialBlock,
                finalBlock = poolInfo.finalBlock,
                fee = poolInfo.fee,
                sharedAssetIssuance = sharedAssetIssuance,
                gson = gson,
                currentBlock = currentBlock,
                pegs = pegs[poolId]?.current ?: StablePool.getDefaultPegs(pooledAssets.size)
            )
        }
    }

    private fun Collection<PoolInitialInfo>.collectAllAssetIds(): List<HydraDxAssetId> {
        return flatMap { pool ->
            buildList {
                add(pool.sharedAsset.first)

                pool.poolAssets.onEach {
                    add(it.first)
                }
            }
        }
    }

    private suspend fun fetchAssetMetadataMap(allAssetIds: List<HydraDxAssetId>): HydrationAssetMetadataMap {
        return remoteStorageSource.query(chain.id) {
            val assetMetadatas = metadata.assetRegistry.assets.multi(allAssetIds).filterNotNull()
            HydrationAssetMetadataMap(
                nativeId = hydraDxAssetIdConverter.systemAssetId,
                metadataMap = assetMetadatas
            )
        }
    }

    private fun stableSwapPoolAccountId(poolId: HydraDxAssetId): AccountId {
        val prefix = "sts".encodeToByteArray()
        val suffix = poolId.toInt().asLittleEndianBytes()

        return (prefix + suffix).blake2b256()
    }

    private suspend fun getPools(): Map<HydraDxAssetId, StableSwapPoolInfo> {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.stableSwapOrNull?.pools?.entries().orEmpty()
        }
    }

    private suspend fun Map<HydraDxAssetId, StableSwapPoolInfo>.matchIdsWithLocal(): List<PoolInitialInfo> {
        val allOnChainIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return mapNotNull outer@{ (poolAssetId, poolInfo) ->
            val poolAssetMatchedId = allOnChainIds[poolAssetId]?.fullId

            val participatingAssetsMatchedIds = poolInfo.assets.map { assetId ->
                val localId = allOnChainIds[assetId]?.fullId

                assetId to localId
            }

            PoolInitialInfo(
                sharedAsset = poolAssetId to poolAssetMatchedId,
                poolAssets = participatingAssetsMatchedIds
            )
        }
    }

    private fun Collection<PoolInitialInfo>.allPossibleDirections(): Collection<RealStableSwapQuotingEdge> {
        return flatMap { (poolAssetId, poolAssets) ->
            val allPoolAssetIds = buildList {
                addAll(poolAssets.mapNotNull { it.flatten() })

                val sharedAssetId = poolAssetId.flatten()

                if (sharedAssetId != null) {
                    add(sharedAssetId)
                }
            }

            allPoolAssetIds.flatMap { assetId ->
                allPoolAssetIds.mapNotNull { otherAssetId ->
                    otherAssetId.takeIf { assetId != otherAssetId }
                        ?.let { RealStableSwapQuotingEdge(assetId, otherAssetId, poolAssetId.first) }
                }
            }
        }
    }

    private data class PoolInitialInfo(
        val sharedAsset: RemoteAndLocalIdOptional,
        val poolAssets: List<RemoteAndLocalIdOptional>
    )

    inner class RealStableSwapQuotingEdge(
        override val fromAsset: RemoteAndLocalId,
        override val toAsset: RemoteAndLocalId,
        override val poolId: HydraDxAssetId
    ) : StableSwapQuotingSource.Edge {

        override val from: FullChainAssetId = fromAsset.second

        override val to: FullChainAssetId = toAsset.second

        override val weight: Int
            get() = Weights.Hydra.STABLESWAP

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            val allPools = stablePools.first()
            val relevantPool = allPools.first { it.sharedAsset.id == poolId }

            return relevantPool.quote(fromAsset.first, toAsset.first, amount, direction)
                ?: throw SwapQuoteException.NotEnoughLiquidity
        }
    }
}
