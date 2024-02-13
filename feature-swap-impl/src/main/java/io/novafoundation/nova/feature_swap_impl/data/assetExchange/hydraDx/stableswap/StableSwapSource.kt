package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap

import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraSwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model.StableSwapPoolInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val POOL_ID_PARAM_KEY = "PoolId"

class StableSwapSourceFactory(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
): HydraDxSwapSource.Factory {

    override fun create(chain: Chain): HydraDxSwapSource {
        return StableSwapSource(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            assetSourceRegistry = assetSourceRegistry,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            chain = chain
        )
    }
}

private class StableSwapSource(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val chain: Chain,
) : HydraDxSwapSource {

    override val identifier: String = "StableSwap"

    override suspend fun availableSwapDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val pools = getPools()

        return pools.allPossibleDirections()
    }

    override suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        // TODO
    }

    override suspend fun quote(args: AssetExchangeQuoteArgs): Balance {
        // TODO
        return Balance.ZERO
    }

    override suspend fun runSubscriptions(userAccountId: AccountId, subscriptionBuilder: SharedRequestsBuilder): Flow<Unit> {
        // TODO
        return emptyFlow()
    }

    override fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*> {
        // TODO add pool id
        return DictEnum.Entry("Stableswap", null)
    }

    private suspend fun getPools(): Map<HydraDxAssetId, StableSwapPoolInfo> {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.stableSwapOrNull?.pools?.entries().orEmpty()
        }
    }

    private suspend fun Map<HydraDxAssetId, StableSwapPoolInfo>.allPossibleDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val allOnChainIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        val perPoolMaps = mapNotNull { (poolAssetId, poolInfo) ->
            // Ignore pool if its pool asset is unknown
            val poolAssetLocalId = allOnChainIds[poolAssetId]?.fullId ?: return@mapNotNull null

            val participatingAssetsLocalIds = poolInfo.assets.map { assetId ->
                // Be defensive here - we potentially may allow to swap using stableswap pool that contain unknown assets to us but for now forbid it
                allOnChainIds[assetId]?.fullId ?: return@mapNotNull null
            }

            val allPoolAssetIds = participatingAssetsLocalIds + poolAssetLocalId

            allPoolAssetIds.associateWith { assetId ->
                allPoolAssetIds.mapNotNull { otherAssetId ->
                    otherAssetId.takeIf { assetId != otherAssetId }?.let { StableSwapDirection(assetId, otherAssetId, poolAssetId) }
                }
            }
        }

        return Graph.create(perPoolMaps).adjacencyList
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
}
