package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.dynamicFees
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.omnipool
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.toMultiSubscription
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.HydrationAssetType
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.HydrationBalanceFetcher
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.HydrationBalanceFetcherFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common.fromAsset
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.DynamicFee
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.OmniPool
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.OmniPoolFees
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.OmniPoolToken
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.OmnipoolAssetState
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteIdAndLocalAsset
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.feeParamsConstant
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.quote
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Named

@FeatureScope
class OmniPoolQuotingSourceFactory @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydrationBalanceFetcherFactory: HydrationBalanceFetcherFactory
) : HydraDxQuotingSource.Factory<OmniPoolQuotingSource> {

    companion object {

        const val SOURCE_ID = "OmniPool"
    }

    override fun create(chain: Chain, host: SwapQuoting.QuotingHost): OmniPoolQuotingSource {
        return RealOmniPoolQuotingSource(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydrationBalanceFetcher = hydrationBalanceFetcherFactory.create(host),
            chain = chain,
        )
    }
}

private class RealOmniPoolQuotingSource(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydrationBalanceFetcher: HydrationBalanceFetcher,
    private val chain: Chain,
) : OmniPoolQuotingSource {

    override val identifier = OmniPoolQuotingSourceFactory.SOURCE_ID

    private val pooledOnChainAssetIdsState: MutableSharedFlow<List<RemoteIdAndLocalAsset>> = singleReplaySharedFlow()

    private val omniPoolFlow: MutableSharedFlow<OmniPool> = singleReplaySharedFlow()

    override suspend fun sync() {
        val pooledOnChainAssetIds = getPooledOnChainAssetIds()

        val pooledChainAssetsIds = matchKnownChainAssetIds(pooledOnChainAssetIds)
        pooledOnChainAssetIdsState.emit(pooledChainAssetsIds)
    }

    override suspend fun availableSwapDirections(): Collection<OmniPoolQuotingSource.Edge> {
        val pooledOnChainAssetIds = pooledOnChainAssetIdsState.first()

        return pooledOnChainAssetIds.flatMap { remoteAndLocal ->
            pooledOnChainAssetIds.mapNotNull { otherRemoteAndLocal ->
                // In OmniPool, each asset is tradable with any other except itself
                if (remoteAndLocal.second.id != otherRemoteAndLocal.second.id) {
                    RealOmniPoolQuotingEdge(fromAsset = remoteAndLocal, toAsset = otherRemoteAndLocal)
                } else {
                    null
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> {
        omniPoolFlow.resetReplayCache()

        val pooledAssets = pooledOnChainAssetIdsState.first()

        val omniPoolStateFlow = pooledAssets.map { (onChainId, _) ->
            remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                metadata.omnipool.assets.observeNonNull(onChainId).map {
                    onChainId to it
                }
            }
        }
            .toMultiSubscription(pooledAssets.size)

        val poolAccountId = omniPoolAccountId()

        val omniPoolBalancesFlow = pooledAssets.map { (omniPoolTokenId, chainAsset) ->
            val hydrationAssetType = HydrationAssetType.fromAsset(chainAsset, omniPoolTokenId)

            hydrationBalanceFetcher.subscribeToTransferableBalance(chainAsset.chainId, hydrationAssetType, poolAccountId, subscriptionBuilder).map {
                omniPoolTokenId to it
            }
        }
            .toMultiSubscription(pooledAssets.size)

        val feesFlow = pooledAssets.map { (omniPoolTokenId, _) ->
            remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                metadata.dynamicFeesApi.assetFee.observe(omniPoolTokenId).map {
                    omniPoolTokenId to it
                }
            }
        }.toMultiSubscription(pooledAssets.size)

        val defaultFees = getDefaultFees()

        return combine(omniPoolStateFlow, omniPoolBalancesFlow, feesFlow) { poolState, poolBalances, fees ->
            createOmniPool(poolState, poolBalances, fees, defaultFees)
        }
            .onEach(omniPoolFlow::emit)
            .map { }
    }

    private suspend fun getPooledOnChainAssetIds(): List<BigInteger> {
        return remoteStorageSource.query(chain.id) {
            val hubAssetId = metadata.omnipool().numberConstant("HubAssetId", runtime)
            val allAssets = runtime.metadata.omnipoolOrNull?.assets?.keys().orEmpty()

            // remove hubAssetId from trading paths
            allAssets.filter { it != hubAssetId }
        }
    }

    private suspend fun matchKnownChainAssetIds(onChainIds: List<HydraDxAssetId>): List<RemoteIdAndLocalAsset> {
        val hydraDxAssetIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return onChainIds.mapNotNull { onChainId ->
            val asset = hydraDxAssetIds[onChainId] ?: return@mapNotNull null

            onChainId to asset
        }
    }

    private fun createOmniPool(
        poolAssetStates: Map<HydraDxAssetId, OmnipoolAssetState>,
        poolBalances: Map<HydraDxAssetId, BigInteger>,
        fees: Map<HydraDxAssetId, DynamicFee?>,
        defaultFees: OmniPoolFees,
    ): OmniPool {
        val tokensState = poolAssetStates.mapValues { (tokenId, poolAssetState) ->
            val assetBalance = poolBalances[tokenId].orZero()
            val tokenFees = fees[tokenId]?.let { OmniPoolFees(it.protocolFee, it.assetFee) } ?: defaultFees

            OmniPoolToken(
                hubReserve = poolAssetState.hubReserve,
                shares = poolAssetState.shares,
                protocolShares = poolAssetState.protocolShares,
                tradeability = poolAssetState.tradeability,
                balance = assetBalance,
                fees = tokenFees
            )
        }

        return OmniPool(tokensState)
    }

    private suspend fun getDefaultFees(): OmniPoolFees {
        val runtime = chainRegistry.getRuntime(chain.id)

        val assetFeeParams = runtime.metadata.dynamicFees().feeParamsConstant("AssetFeeParameters", runtime)
        val protocolFeeParams = runtime.metadata.dynamicFees().feeParamsConstant("ProtocolFeeParameters", runtime)

        return OmniPoolFees(
            protocolFee = protocolFeeParams.minFee,
            assetFee = assetFeeParams.minFee
        )
    }

    private inner class RealOmniPoolQuotingEdge(
        override val fromAsset: RemoteIdAndLocalAsset,
        override val toAsset: RemoteIdAndLocalAsset,
    ) : OmniPoolQuotingSource.Edge {

        override val from: FullChainAssetId = fromAsset.second.fullId

        override val to: FullChainAssetId = toAsset.second.fullId

        override val weight: Int
            get() = Weights.Hydra.OMNIPOOL

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            val omniPool = omniPoolFlow.first()

            return omniPool.quote(fromAsset.first, toAsset.first, amount, direction)
                ?: throw SwapQuoteException.NotEnoughLiquidity
        }
    }
}
