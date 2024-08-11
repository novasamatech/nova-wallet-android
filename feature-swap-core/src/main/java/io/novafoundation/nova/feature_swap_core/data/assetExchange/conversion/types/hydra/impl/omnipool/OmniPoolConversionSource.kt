package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool

import io.novafoundation.nova.common.data.network.ext.transferableBalance
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrmlAccountBalanceOrEmpty
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.common.utils.dynamicFees
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.omnipool
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.padEnd
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.toMultiSubscription
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxConversionSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxConversionSourceQuoteArgs
import io.novafoundation.nova.feature_swap_core.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxSwapSourceId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraSwapDirection
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.model.DynamicFee
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.model.OmniPool
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.model.OmniPoolFees
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.model.OmniPoolToken
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.model.OmnipoolAssetState
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.model.feeParamsConstant
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.model.quote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.subscribeToTransferableBalance
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novafoundation.nova.runtime.storage.typed.account
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

class OmniPoolConversionSourceFactory(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxConversionSource.Factory {

    companion object {

        const val SOURCE_ID = "OmniPool"
    }

    override fun create(chain: Chain): HydraDxConversionSource {
        return OmniPoolConversionSource(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            chain = chain
        )
    }
}

private class OmniPoolConversionSource(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val chain: Chain,
) : HydraDxConversionSource {

    override val identifier: HydraDxSwapSourceId = OmniPoolConversionSourceFactory.SOURCE_ID

    private val pooledOnChainAssetIdsState: MutableSharedFlow<List<RemoteAndLocalId>> = singleReplaySharedFlow()

    private val omniPoolFlow: MutableSharedFlow<OmniPool> = singleReplaySharedFlow()

    override suspend fun availableSwapDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection> {
        val pooledOnChainAssetIds = getPooledOnChainAssetIds()

        val pooledChainAssetsIds = matchKnownChainAssetIds(pooledOnChainAssetIds)
        pooledOnChainAssetIdsState.emit(pooledChainAssetsIds)

        return pooledChainAssetsIds.associateBy(
            keySelector = { it.second },
            valueTransform = { (_, currentId) ->
                // In OmniPool, each asset is tradable with any other except itself
                pooledChainAssetsIds.mapNotNull { (_, otherId) ->
                    otherId.takeIf { currentId != otherId }?.let { OmniPoolSwapDirection(currentId, otherId) }
                }
            }
        )
    }


    override suspend fun quote(args: HydraDxConversionSourceQuoteArgs): BigInteger {
        val omniPool = omniPoolFlow.first()

        val omniPoolTokenIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetIn)
        val omniPoolTokenIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.chainAssetOut)

        return omniPool.quote(omniPoolTokenIdIn, omniPoolTokenIdOut, args.amount, args.swapDirection)
            ?: throw SwapQuoteException.NotEnoughLiquidity
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

        val omniPoolBalancesFlow = pooledAssets.map { (omniPoolTokenId, chainAssetId) ->
            val chainAsset = chain.assetsById.getValue(chainAssetId.assetId)
            remoteStorageSource.subscribeToTransferableBalance(chainAsset, poolAccountId, subscriptionBuilder).map {
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

    override fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*> {
        return DictEnum.Entry("Omnipool", null)
    }

    private suspend fun getPooledOnChainAssetIds(): List<BigInteger> {
        return remoteStorageSource.query(chain.id) {
            val hubAssetId = metadata.omnipool().numberConstant("HubAssetId", runtime)
            val allAssets = runtime.metadata.omnipoolOrNull?.assets?.keys().orEmpty()

            // remove hubAssetId from trading paths
            allAssets.filter { it != hubAssetId }
        }
    }

    private suspend fun matchKnownChainAssetIds(onChainIds: List<HydraDxAssetId>): List<RemoteAndLocalId> {
        val hydraDxAssetIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return onChainIds.mapNotNull { onChainId ->
            val asset = hydraDxAssetIds[onChainId] ?: return@mapNotNull null

            onChainId to asset.fullId
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

    private fun ExtrinsicBuilder.sell(
        assetIdIn: HydraDxAssetId,
        assetIdOut: HydraDxAssetId,
        amountIn: BigInteger,
        minBuyAmount: BigInteger
    ) {
        call(
            moduleName = Modules.OMNIPOOL,
            callName = "sell",
            arguments = mapOf(
                "asset_in" to assetIdIn,
                "asset_out" to assetIdOut,
                "amount" to amountIn,
                "min_buy_amount" to minBuyAmount
            )
        )
    }

    private fun ExtrinsicBuilder.buy(
        assetIdIn: HydraDxAssetId,
        assetIdOut: HydraDxAssetId,
        amountOut: BigInteger,
        maxSellAmount: BigInteger
    ) {
        call(
            moduleName = Modules.OMNIPOOL,
            callName = "buy",
            arguments = mapOf(
                "asset_out" to assetIdOut,
                "asset_in" to assetIdIn,
                "amount" to amountOut,
                "max_sell_amount" to maxSellAmount
            )
        )
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

    private class OmniPoolSwapDirection(override val from: FullChainAssetId, override val to: FullChainAssetId) : HydraSwapDirection {

        override val params: Map<String, String>
            get() = emptyMap()
    }
}

fun omniPoolAccountId(): AccountId {
    return "modlomnipool".encodeToByteArray().padEnd(expectedSize = 32, padding = 0)
}

typealias RemoteAndLocalId = Pair<HydraDxAssetId, FullChainAssetId>

val RemoteAndLocalId.remoteId
    get() = first

val RemoteAndLocalId.localId
    get() = second

typealias RemoteAndLocalIdOptional = Pair<HydraDxAssetId, FullChainAssetId?>
