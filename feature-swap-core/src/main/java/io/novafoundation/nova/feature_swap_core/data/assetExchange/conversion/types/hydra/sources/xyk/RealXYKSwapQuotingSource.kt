package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.xyk
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.localId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.subscribeToTransferableBalance
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.model.XYKPool
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.model.XYKPoolAsset
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.model.XYKPoolInfo
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.model.XYKPools
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.model.poolFeesConstant
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class XYKSwapQuotingSourceFactory(
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter
) : HydraDxQuotingSource.Factory<XYKSwapQuotingSource> {

    companion object {

        const val ID = "XYK"
    }

    override fun create(chain: Chain, host: SwapQuoting.QuotingHost): XYKSwapQuotingSource {
        return RealXYKSwapQuotingSource(
            remoteStorageSource = remoteStorageSource,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            chain = chain
        )
    }
}

private class RealXYKSwapQuotingSource(
    private val remoteStorageSource: StorageDataSource,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val chain: Chain
) : XYKSwapQuotingSource {

    override val identifier: String = XYKSwapQuotingSourceFactory.ID

    private val initialPoolsInfo: MutableSharedFlow<Collection<PoolInitialInfo>> = singleReplaySharedFlow()

    private val xykPools: MutableSharedFlow<XYKPools> = singleReplaySharedFlow()

    override suspend fun sync() {
        val pools = getPools()

        val poolInitialInfo = pools.matchIdsWithLocal()
        initialPoolsInfo.emit(poolInitialInfo)
    }

    override suspend fun availableSwapDirections(): Collection<XYKSwapQuotingSource.Edge> {
        val poolInitialInfo = initialPoolsInfo.first()

        return poolInitialInfo.allPossibleDirections()
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

    private suspend fun subscribeToBalance(
        assetId: RemoteAndLocalId,
        poolAddress: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BigInteger> {
        val chainAsset = chain.assetsById.getValue(assetId.localId.assetId)

        return remoteStorageSource.subscribeToTransferableBalance(chainAsset, poolAddress, subscriptionBuilder)
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

    private fun Collection<PoolInitialInfo>.allPossibleDirections(): Collection<RealXYKSwapQuotingEdge> {
        return buildList {
            this@allPossibleDirections.forEach { poolInfo ->
                add(
                    RealXYKSwapQuotingEdge(
                        fromAsset = poolInfo.firstAsset,
                        toAsset = poolInfo.secondAsset,
                        poolAddress = poolInfo.poolAddress
                    )
                )

                add(
                    RealXYKSwapQuotingEdge(
                        fromAsset = poolInfo.secondAsset,
                        toAsset = poolInfo.firstAsset,
                        poolAddress = poolInfo.poolAddress
                    )
                )
            }
        }
    }

    inner class RealXYKSwapQuotingEdge(
        override val fromAsset: RemoteAndLocalId,
        override val toAsset: RemoteAndLocalId,
        override val poolAddress: AccountId
    ) : XYKSwapQuotingSource.Edge {

        override val from: FullChainAssetId = fromAsset.second

        override val to: FullChainAssetId = toAsset.second

        override val weight: Int
            get() = Weights.Hydra.XYK

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            val allPools = xykPools.first()

            return allPools.quote(poolAddress, fromAsset.first, toAsset.first, amount, direction)
                ?: throw SwapQuoteException.NotEnoughLiquidity
        }
    }
}

private class PoolInitialInfo(
    val poolAddress: AccountId,
    val firstAsset: RemoteAndLocalId,
    val secondAsset: RemoteAndLocalId
)
