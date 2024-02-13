package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.toMultiSubscription
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSourceQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraSwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.omniPoolAccountId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model.StablePool
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model.StablePoolAsset
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model.StableSwapPoolInfo
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model.quote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.toOnChainIdOrThrow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.encrypt.json.asLittleEndianBytes
import jp.co.soramitsu.fearless_utils.hash.Hasher.blake2b256
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private const val POOL_ID_PARAM_KEY = "PoolId"

class StableSwapSourceFactory(
    private val remoteStorageSource: StorageDataSource,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val gson: Gson,
    private val chainStateRepository: ChainStateRepository
) : HydraDxSwapSource.Factory {

    override fun create(chain: Chain): HydraDxSwapSource {
        return StableSwapSource(
            remoteStorageSource = remoteStorageSource,
            assetSourceRegistry = assetSourceRegistry,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            chain = chain,
            gson = gson,
            chainStateRepository = chainStateRepository
        )
    }
}

private class StableSwapSource(
    private val remoteStorageSource: StorageDataSource,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val chain: Chain,
    private val gson: Gson,
    private val chainStateRepository: ChainStateRepository,
) : HydraDxSwapSource {

    override val identifier: String = "StableSwap"

    private val initialPoolsInfo: MutableSharedFlow<Collection<PoolInitialInfo>> = singleReplaySharedFlow()

    private val stablePools: MutableSharedFlow<List<StablePool>> = singleReplaySharedFlow()

    override suspend fun availableSwapDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val pools = getPools()

        val poolInitialInfo = pools.matchIdsWithLocal()
        initialPoolsInfo.emit(poolInitialInfo)

        return poolInitialInfo.allPossibleDirections()
    }

    override suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        // We don't need a specific implementation for StableSwap extrinsics since it is done by HydraDxExchange on the upper level via Router
    }

    override suspend fun quote(args: HydraDxSwapSourceQuoteArgs): Balance {
        val allPools = stablePools.first()
        val poolId = args.params.poolIdParam()
        val relevantPool = allPools.first { it.sharedAsset.id == poolId }

        val omniPoolTokenIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetIn)
        val omniPoolTokenIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetOut)

        return relevantPool.quote(omniPoolTokenIdIn, omniPoolTokenIdOut, args.amount, args.swapDirection)
            ?: throw SwapQuoteException.NotEnoughLiquidity
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> {
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
            val chainAsset = chain.assetsById.getValue(poolInfo.sharedAsset.second.assetId)
            val assetSource = assetSourceRegistry.sourceFor(chainAsset)
            assetSource.balance.subscribeTransferableAccountBalance(chain, chainAsset, omniPoolAccountId, subscriptionBuilder).map {
                poolInfo.sharedAsset.first to it
            }
        }.toMultiSubscription(initialPoolsInfo.size)

        val totalPooledAssets = initialPoolsInfo.sumOf { it.poolAssets.size }

        val poolParticipatingAssetsBalanceSubscription = initialPoolsInfo.flatMap { poolInfo ->
            val poolAccountId = stableSwapPoolAccountId(poolInfo.sharedAsset.first)

            poolInfo.poolAssets.map { poolAsset ->
                val chainAsset = chain.assetsById.getValue(poolAsset.second.assetId)
                val assetSource = assetSourceRegistry.sourceFor(chainAsset)
                assetSource.balance.subscribeTransferableAccountBalance(chain, chainAsset, poolAccountId, subscriptionBuilder).map {
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

        return combine(
            poolInfoSubscriptions,
            poolSharedAssetBalanceSubscriptions,
            poolParticipatingAssetsBalanceSubscription,
            totalIssuanceSubscriptions,
            chainStateRepository.currentBlockNumberFlow(chain.id),
            ::createStableSwapPool
        )
            .onEach(stablePools::emit)
            .map { }
    }

    private suspend fun createStableSwapPool(
        poolInfos: Map<HydraDxAssetId, StableSwapPoolInfo?>,
        poolSharedAssetBalances: Map<HydraDxAssetId, Balance>,
        poolParticipatingAssetBalances: Map<Pair<HydraDxAssetId, HydraDxAssetId>, Balance>,
        totalIssuances: Map<HydraDxAssetId, Balance>,
        currentBlock: BlockNumber
    ): List<StablePool> {
        val allLocalIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return poolInfos.mapNotNull outer@{ (poolId, poolInfo) ->
            if (poolInfo == null) return@outer null

            val sharedAssetBalance = poolSharedAssetBalances[poolId].orZero()
            val sharedChainAsset = allLocalIds[poolId] ?: return@outer null
            val sharedAsset = StablePoolAsset(sharedAssetBalance, poolId, sharedChainAsset.precision)
            val sharedAssetIssuance = totalIssuances[poolId].orZero()

            val pooledAssets = poolInfo.assets.mapNotNull { pooledAssetId ->
                val pooledAssetBalance = poolParticipatingAssetBalances[poolId to pooledAssetId].orZero()
                val pooledChainAsset = allLocalIds[pooledAssetId] ?: return@mapNotNull null
                val decimals = pooledChainAsset.precision

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

    private fun stableSwapPoolAccountId(poolId: HydraDxAssetId): AccountId {
        val prefix = "sts".encodeToByteArray()
        val suffix = poolId.toInt().asLittleEndianBytes()

        return (prefix + suffix).blake2b256()
    }

    override fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*> {
        val poolId = params.getValue(POOL_ID_PARAM_KEY).toBigInteger()

        return DictEnum.Entry("Stableswap", poolId)
    }

    private suspend fun getPools(): Map<HydraDxAssetId, StableSwapPoolInfo> {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.stableSwapOrNull?.pools?.entries().orEmpty()
        }
    }

    private suspend fun Map<HydraDxAssetId, StableSwapPoolInfo>.matchIdsWithLocal(): List<PoolInitialInfo> {
        val allOnChainIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return mapNotNull { (poolAssetId, poolInfo) ->
            // Ignore pool if its pool asset is unknown
            val poolAssetMatchedId = allOnChainIds[poolAssetId]?.fullId ?: return@mapNotNull null

            val participatingAssetsMatchedIds = poolInfo.assets.map { assetId ->
                // Be defensive here - we potentially may allow to swap using stableswap pool that contain unknown assets to us but for now forbid it
                val localId = allOnChainIds[assetId]?.fullId ?: return@mapNotNull null

                assetId to localId
            }

            PoolInitialInfo(
                sharedAsset = poolAssetId to poolAssetMatchedId,
                poolAssets = participatingAssetsMatchedIds
            )
        }
    }

    private fun List<PoolInitialInfo>.allPossibleDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val perPoolMaps = map { (poolAssetId, poolAssets) ->
            val allPoolAssetIds = poolAssets.map { it.second } + poolAssetId.second

            allPoolAssetIds.associateWith { assetId ->
                allPoolAssetIds.mapNotNull { otherAssetId ->
                    otherAssetId.takeIf { assetId != otherAssetId }?.let { StableSwapDirection(assetId, otherAssetId, poolAssetId.first) }
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
        val sharedAsset: RemoteAndLocalId,
        val poolAssets: List<RemoteAndLocalId>
    )
}
