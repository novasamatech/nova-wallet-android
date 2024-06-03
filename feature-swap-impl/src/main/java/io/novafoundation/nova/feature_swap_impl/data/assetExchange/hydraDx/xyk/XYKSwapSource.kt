package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.GraphBuilder
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.xyk
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSourceQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraSwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.localId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model.XYKPool
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model.XYKPoolAsset
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model.XYKPoolInfo
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model.XYKPools
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model.poolFeesConstant
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.toOnChainIdOrThrow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val POOL_ID_PARAM_KEY = "PoolId"

class XYKSwapSourceFactory(
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val assetSourceRegistry: AssetSourceRegistry,
) : HydraDxSwapSource.Factory {

    override fun create(chain: Chain): HydraDxSwapSource {
        return XYKSwapSource(
            remoteStorageSource = remoteStorageSource,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            chain = chain,
            assetSourceRegistry = assetSourceRegistry
        )
    }
}

private class XYKSwapSource(
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val chain: Chain,
    private val assetSourceRegistry: AssetSourceRegistry,
) : HydraDxSwapSource {

    override val identifier: String = "Xyk"

    private val initialPoolsInfo: MutableSharedFlow<Collection<PoolInitialInfo>> = singleReplaySharedFlow()

    private val xykPools: MutableSharedFlow<XYKPools> = singleReplaySharedFlow()

    override suspend fun availableSwapDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val pools = getPools()

        val poolInitialInfo = pools.matchIdsWithLocal()
        initialPoolsInfo.emit(poolInitialInfo)

        return poolInitialInfo.allPossibleDirections()
    }

    override suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        // We don't need a specific implementation for XYKSwap extrinsics since it is done by HydraDxExchange on the upper level via Router
    }

    override suspend fun quote(args: HydraDxSwapSourceQuoteArgs): Balance {
        val allPools = xykPools.first()
        val poolAddress = args.params.poolAddressParam()

        val hydraDxAssetIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetIn)
        val hydraDxAssetIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetOut)

        return allPools.quote(poolAddress, hydraDxAssetIdIn, hydraDxAssetIdOut, args.amount, args.swapDirection)
            ?: throw SwapQuoteException.NotEnoughLiquidity
    }

    private suspend fun subscribeToBalance(
        assetId: RemoteAndLocalId,
        poolAddress: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Balance> {
        val chainAsset = chain.assetsById.getValue(assetId.localId.assetId)
        val assetSource = assetSourceRegistry.sourceFor(chainAsset)

        return assetSource.balance.subscribeTransferableAccountBalance(chain, chainAsset, poolAddress, subscriptionBuilder)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> = coroutineScope {
        xykPools.resetReplayCache()

        val initialPoolsInfo = initialPoolsInfo.first()

        val poolsSubscription = initialPoolsInfo.map { poolInfo ->
            val firstBalanceFlow = subscribeToBalance(poolInfo.firstAsset, poolInfo.poolAddress, subscriptionBuilder)
            val secondBalanceFlow = subscribeToBalance(poolInfo.secondAsset, poolInfo.poolAddress, subscriptionBuilder)

            firstBalanceFlow.combine(secondBalanceFlow) { firstBalance, secondBalance ->
                XYKPool(
                    address = poolInfo.poolAddress,
                    firstAsset = XYKPoolAsset(firstBalance, poolInfo.firstAsset.first),
                    secondAsset = XYKPoolAsset(secondBalance, poolInfo.secondAsset.first),
                )
            }
        }.combine()

        val fees = remoteStorageSource.query(chain.id) {
            runtime.metadata.xyk().poolFeesConstant(runtime)
        }

        poolsSubscription.map { pools ->
            val built = XYKPools(fees, pools)
            xykPools.emit(built)
        }
    }

    override fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*> {
        return DictEnum.Entry("XYK", null)
    }

    private suspend fun getPools(): Map<AccountIdKey, XYKPoolInfo> {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.xykOrNull?.poolAssets?.entries().orEmpty()
        }
    }

    private suspend fun Map<AccountIdKey, XYKPoolInfo>.matchIdsWithLocal(): List<PoolInitialInfo> {
        val allOnChainIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        fun matchId(remoteId: HydraDxAssetId): RemoteAndLocalId? {
            return allOnChainIds[remoteId]?.fullId?.let {
                remoteId to it
            }
        }

        return mapNotNull outer@{ (poolAddress, poolInfo) ->
            PoolInitialInfo(
                poolAddress = poolAddress.value,
                firstAsset = matchId(poolInfo.firstAsset) ?: return@outer null,
                secondAsset = matchId(poolInfo.secondAsset) ?: return@outer null,
            )
        }
    }

    private fun List<PoolInitialInfo>.allPossibleDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val builder = GraphBuilder<FullChainAssetId, HYKSwapDirection>()

        onEach { poolInfo ->
            builder.addEdge(
                from = poolInfo.firstAsset.localId,
                to = HYKSwapDirection(
                    from = poolInfo.firstAsset.localId,
                    to = poolInfo.secondAsset.localId,
                    poolAddress = poolInfo.poolAddress
                )
            )
            builder.addEdge(
                from = poolInfo.secondAsset.localId,
                to = HYKSwapDirection(
                    from = poolInfo.secondAsset.localId,
                    to = poolInfo.firstAsset.localId,
                    poolAddress = poolInfo.poolAddress
                )
            )
        }

        return builder.build().adjacencyList
    }

    private fun Map<String, String>.poolAddressParam(): AccountId {
        return getValue(POOL_ID_PARAM_KEY).fromHex()
    }

    private class HYKSwapDirection(
        override val from: FullChainAssetId,
        override val to: FullChainAssetId,
        poolAddress: AccountId
    ) : HydraSwapDirection, Edge<FullChainAssetId> {

        val poolAddressRaw = poolAddress.toHexString()

        override val params: Map<String, String>
            get() = mapOf(POOL_ID_PARAM_KEY to poolAddressRaw)
    }
}

private class PoolInitialInfo(
    val poolAddress: AccountId,
    val firstAsset: RemoteAndLocalId,
    val secondAsset: RemoteAndLocalId
)
