package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.orEmpty
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.toMultiSubscription
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxConversionSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxConversionSourceQuoteArgs
import io.novafoundation.nova.feature_swap_core.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraSwapDirection
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.RemoteAndLocalIdOptional
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.omniPoolAccountId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap.model.StablePool
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap.model.StablePoolAsset
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap.model.StableSwapPoolInfo
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap.model.quote
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.encrypt.json.asLittleEndianBytes
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private const val POOL_ID_PARAM_KEY = "PoolId"

class StableConversionSourceFactory(
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val gson: Gson,
    private val chainStateRepository: ChainStateRepository
) : HydraDxConversionSource.Factory {

    companion object {
        const val ID = "StableSwap"
    }

    override fun create(chain: Chain): HydraDxConversionSource {
        return StableConversionSource(
            remoteStorageSource = remoteStorageSource,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            chain = chain,
            gson = gson,
            chainStateRepository = chainStateRepository
        )
    }
}

private class StableConversionSource(
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val chain: Chain,
    private val gson: Gson,
    private val chainStateRepository: ChainStateRepository,
) : HydraDxConversionSource {

    override val identifier: String = StableConversionSourceFactory.ID

    private val initialPoolsInfo: MutableSharedFlow<Collection<PoolInitialInfo>> = singleReplaySharedFlow()

    private val stablePools: MutableSharedFlow<List<StablePool>> = singleReplaySharedFlow()

    override suspend fun sync() {
        val pools = getPools()

        val poolInitialInfo = pools.matchIdsWithLocal()
        initialPoolsInfo.emit(poolInitialInfo)
    }

    override suspend fun availableSwapDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val poolInitialInfo = initialPoolsInfo.first()

        return poolInitialInfo.allPossibleDirections()
    }

    override suspend fun quote(args: HydraDxConversionSourceQuoteArgs): BigInteger {
        val allPools = stablePools.first()
        val poolId = args.params.poolIdParam()
        val relevantPool = allPools.first { it.sharedAsset.id == poolId }

        val hydraDxAssetIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetIn)
        val hydraDxAssetIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetOut)

        return relevantPool.quote(hydraDxAssetIdIn, hydraDxAssetIdOut, args.amount, args.swapDirection)
            ?: throw SwapQuoteException.NotEnoughLiquidity
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

        val poolSharedAssetBalanceSubscriptions = initialPoolsInfo.map { poolInfo ->
            val sharedAssetRemoteId = poolInfo.sharedAsset.first

            subscribeTransferableBalance(subscriptionBuilder, omniPoolAccountId, sharedAssetRemoteId).map {
                sharedAssetRemoteId to it
            }
        }.toMultiSubscription(initialPoolsInfo.size)

        val totalPooledAssets = initialPoolsInfo.sumOf { it.poolAssets.size }

        val poolParticipatingAssetsBalanceSubscription = initialPoolsInfo.flatMap { poolInfo ->
            val poolAccountId = stableSwapPoolAccountId(poolInfo.sharedAsset.first)

            poolInfo.poolAssets.map { poolAsset ->
                subscribeTransferableBalance(subscriptionBuilder, poolAccountId, poolAsset.first).map {
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

        val precisions = fetchAssetsPrecisionsAsync()

        combine(
            poolInfoSubscriptions,
            poolSharedAssetBalanceSubscriptions,
            poolParticipatingAssetsBalanceSubscription,
            totalIssuanceSubscriptions,
            chainStateRepository.currentBlockNumberFlow(chain.id),
        ) { poolInfos, poolSharedAssetBalances, poolParticipatingAssetBalances, totalIssuances, currentBlock ->
            createStableSwapPool(poolInfos, poolSharedAssetBalances, poolParticipatingAssetBalances, totalIssuances, currentBlock, precisions.await())
        }
            .onEach(stablePools::emit)
            .map { }
    }

    private suspend fun subscribeTransferableBalance(
        subscriptionBuilder: SharedRequestsBuilder,
        account: AccountId,
        assetId: HydraDxAssetId
    ): Flow<BigInteger> {
        // We cant use AssetSource since it require Chain.Asset which might not always be present in case some stable pool assets are not yet in Nova configs
        return remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
            metadata.hydraTokens.accounts.observe(account, assetId).map {
                TransferableMode.REGULAR.calculateTransferable(it.orEmpty())
            }
        }
    }

    private fun createStableSwapPool(
        poolInfos: Map<HydraDxAssetId, StableSwapPoolInfo?>,
        poolSharedAssetBalances: Map<HydraDxAssetId, BigInteger>,
        poolParticipatingAssetBalances: Map<Pair<HydraDxAssetId, HydraDxAssetId>, BigInteger>,
        totalIssuances: Map<HydraDxAssetId, BigInteger>,
        currentBlock: BlockNumber,
        precisions: Map<HydraDxAssetId, Int>
    ): List<StablePool> {
        return poolInfos.mapNotNull outer@{ (poolId, poolInfo) ->
            if (poolInfo == null) return@outer null

            val sharedAssetBalance = poolSharedAssetBalances[poolId].orZero()
            val sharedChainAssetPrecision = precisions[poolId] ?: return@outer null
            val sharedAsset = StablePoolAsset(sharedAssetBalance, poolId, sharedChainAssetPrecision)
            val sharedAssetIssuance = totalIssuances[poolId].orZero()

            val pooledAssets = poolInfo.assets.mapNotNull { pooledAssetId ->
                val pooledAssetBalance = poolParticipatingAssetBalances[poolId to pooledAssetId].orZero()
                val decimals = precisions[pooledAssetId] ?: return@mapNotNull null

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
                currentBlock = currentBlock
            )
        }
    }

    private fun CoroutineScope.fetchAssetsPrecisionsAsync(): Deferred<Map<HydraDxAssetId, Int>> {
        return async {
            remoteStorageSource.query(chain.id) {
                metadata.assetRegistry.assetMetadataMap.entries().filterNotNull()
            }
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

    private fun Collection<PoolInitialInfo>.allPossibleDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val perPoolMaps = map { (poolAssetId, poolAssets) ->
            val allPoolAssetIds = buildList {
                addAll(poolAssets.mapNotNull { it.second })

                val sharedAssetId = poolAssetId.second

                if (sharedAssetId != null) {
                    add(sharedAssetId)
                }
            }

            allPoolAssetIds.associateWith { assetId ->
                allPoolAssetIds.mapNotNull { otherAssetId ->
                    otherAssetId.takeIf { assetId != otherAssetId }
                        ?.let { StableSwapDirection(assetId, otherAssetId, poolAssetId.first) }
                }
            }
        }

        return Graph.create(perPoolMaps).adjacencyList
    }

    private fun Map<String, String>.poolIdParam(): HydraDxAssetId {
        return getValue(POOL_ID_PARAM_KEY).toBigInteger()
    }

    private class StableSwapDirection(
        override val from: FullChainAssetId,
        override val to: FullChainAssetId,
        poolId: HydraDxAssetId
    ) : HydraSwapDirection, Edge<FullChainAssetId> {
        val poolIdRaw = poolId.toString()

        override val params: Map<String, String>
            get() = mapOf(POOL_ID_PARAM_KEY to poolIdRaw)
    }

    private data class PoolInitialInfo(
        val sharedAsset: RemoteAndLocalIdOptional,
        val poolAssets: List<RemoteAndLocalIdOptional>
    )
}
