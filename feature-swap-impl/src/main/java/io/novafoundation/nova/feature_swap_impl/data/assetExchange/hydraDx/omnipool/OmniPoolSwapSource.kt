package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.common.utils.dynamicFees
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.omnipool
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.padEnd
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.toMultiSubscription
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSourceId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSourceQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraSwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.DynamicFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPool
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolFees
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolToken
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmnipoolAssetState
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.feeParamsConstant
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.quote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_account_api.data.network.hydration.HydraDxAssetId
import io.novafoundation.nova.feature_account_api.data.network.hydration.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_account_api.data.network.hydration.toOnChainIdOrThrow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

class OmniPoolSwapSourceFactory(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : HydraDxSwapSource.Factory {

    companion object {

        const val SOURCE_ID = "OmniPool"
    }

    override fun create(chain: Chain): HydraDxSwapSource {
        return OmniPoolSwapSource(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            assetSourceRegistry = assetSourceRegistry,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            chain = chain
        )
    }
}

private class OmniPoolSwapSource(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val chain: Chain,
) : HydraDxSwapSource {

    override val identifier: HydraDxSwapSourceId = OmniPoolSwapSourceFactory.SOURCE_ID

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

    override suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        val assetIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetIn)
        val assetIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetOut)

        when (val limit = args.swapLimit) {
            is SwapLimit.SpecifiedIn -> sell(
                assetIdIn = assetIdIn,
                assetIdOut = assetIdOut,
                amountIn = limit.expectedAmountIn,
                minBuyAmount = limit.amountOutMin
            )
            is SwapLimit.SpecifiedOut -> buy(
                assetIdIn = assetIdIn,
                assetIdOut = assetIdOut,
                amountOut = limit.expectedAmountOut,
                maxSellAmount = limit.amountInMax
            )
        }
    }

    override suspend fun quote(args: HydraDxSwapSourceQuoteArgs): Balance {
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
            val assetSource = assetSourceRegistry.sourceFor(chainAsset)
            assetSource.balance.subscribeTransferableAccountBalance(chain, chainAsset, poolAccountId, subscriptionBuilder).map {
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
        poolBalances: Map<HydraDxAssetId, Balance>,
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
        amountIn: Balance,
        minBuyAmount: Balance
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
        amountOut: Balance,
        maxSellAmount: Balance
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
