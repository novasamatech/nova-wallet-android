package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.omnipool
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.padEnd
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPool
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolToken
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolTokenId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmnipoolAssetState
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.quote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.decodeOrNull
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import java.math.BigInteger

private val SYSTEM_ON_CHAIN_ASSET_ID = BigInteger.ZERO

class HydraDxOmnipoolExchangeFactory(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
) : AssetExchange.Factory {

    override suspend fun create(chain: Chain, coroutineScope: CoroutineScope): AssetExchange {
        return HydraDxOmnipoolExchange(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            chain = chain,
            storageSharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
            assetSourceRegistry = assetSourceRegistry,
            extrinsicService = extrinsicService
        )
    }
}

private class HydraDxOmnipoolExchange(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val chain: Chain,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
) : AssetExchange {

    private val pooledOnChainAssetIdsState: MutableSharedFlow<List<RemoteAndLocalId>> = singleReplaySharedFlow()

    private val omniPoolFlow: MutableSharedFlow<OmniPool> = singleReplaySharedFlow()

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        // TODO HydraDx supports custom token fee payment
        return false
    }

    override suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId> {
        val pooledOnChainAssetIds = getPooledOnChainAssetIds()

        val pooledChainAssetsIds = matchKnownChainAssetIds(pooledOnChainAssetIds)
        pooledOnChainAssetIdsState.emit(pooledChainAssetsIds)

        return pooledChainAssetsIds.associateBy(
            keySelector = { it.second },
            valueTransform = { (_, currentId) ->
                // In OmniPool, each asset is tradable with any other except itself
                pooledChainAssetsIds.mapNotNullToSet { (_, otherId) -> otherId.takeIf { currentId != otherId } }
            }
        )
    }

    override suspend fun quote(args: SwapQuoteArgs): AssetExchangeQuote {
        val runtime = chainRegistry.getRuntime(chain.id)
        val omniPool = omniPoolFlow.first()

        val omniPoolTokenIdIn = args.tokenIn.configuration.requireOmniPoolTokenId(runtime)
        val omniPoolTokenIdOut = args.tokenOut.configuration.requireOmniPoolTokenId(runtime)

        val quote = omniPool.quote(omniPoolTokenIdIn, omniPoolTokenIdOut, args.amount, args.swapDirection)
            ?: throw SwapQuoteException.NotEnoughLiquidity

        return AssetExchangeQuote(quote)
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee {
        val fee = extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            executeSwap(args)
        }

        return AssetExchangeFee(networkFee = fee, MinimumBalanceBuyIn.NoBuyInNeeded)
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        return extrinsicService.submitExtrinsic(chain, TransactionOrigin.SelectedWallet) {
            executeSwap(args)
        }
    }

    override suspend fun slippageConfig(): SlippageConfig {
        return SlippageConfig.default()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun runSubscriptions(chain: Chain): Flow<ReQuoteTrigger> {
        omniPoolFlow.resetReplayCache()

        return withFlowScope { scope ->
            val pooledAssets = pooledOnChainAssetIdsState.first()
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)

            val omniPoolState = pooledAssets.map { (onChainId, _) ->
                remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                    metadata.omnipool.assets.observeNonNull(onChainId).map {
                        it.tokenId to it
                    }
                }
            }
                .toMultiSubscription(pooledAssets.size)

            val poolAccountId = poolAccountId()

            val omniPoolBalances = pooledAssets.map { (omniPoolTokenId, chainAssetId) ->
                val chainAsset = chain.assetsById.getValue(chainAssetId.assetId)
                val assetSource = assetSourceRegistry.sourceFor(chainAsset)
                assetSource.balance.subscribeTransferableAccountBalance(chain, chainAsset, poolAccountId, subscriptionBuilder).map {
                    omniPoolTokenId to it
                }
            }
                .toMultiSubscription(pooledAssets.size)

            subscriptionBuilder.subscribe(scope)


            combine(omniPoolState, omniPoolBalances, ::createOmniPool)
                .onEach { omniPoolFlow.emit(it) }
                .map { ReQuoteTrigger }
        }
    }

    private fun createOmniPool(
        poolAssetStates: Map<OmniPoolTokenId, OmnipoolAssetState>,
        poolBalances: Map<OmniPoolTokenId, Balance>,
    ): OmniPool {
        val tokensState = poolAssetStates.mapValues { (tokenId, poolAssetState) ->
            val assetBalance = poolBalances[tokenId].orZero()

            OmniPoolToken(
                hubReserve = poolAssetState.hubReserve,
                shares = poolAssetState.shares,
                protocolShares = poolAssetState.protocolShares,
                tradeability = poolAssetState.tradeability,
                balance = assetBalance
            )
        }

        return OmniPool(tokensState)
    }

    private fun <K, V> List<Flow<Pair<K, V>>>.toMultiSubscription(expectedSize: Int): Flow<Map<K, V>> {
        return mergeIfMultiple()
            .runningFold(emptyMap<K, V>()) { accumulator, tokenIdWithBalance ->
                accumulator + tokenIdWithBalance
            }
            .filter { it.size == expectedSize }
    }

    private fun poolAccountId(): AccountId {
        return "modlomnipool".encodeToByteArray().padEnd(expectedSize = 32, padding = 0)
    }

    private suspend fun getPooledOnChainAssetIds(): List<BigInteger> {
        return remoteStorageSource.query(chain.id) {
            val hubAssetId = metadata.omnipool().numberConstant("HubAssetId", runtime)
            val allAssets = runtime.metadata.omnipoolOrNull?.assets?.keys().orEmpty()

            // remove hubAssetId from trading paths
            allAssets.filter { it != hubAssetId }
        }
    }

    private suspend fun matchKnownChainAssetIds(onChainIds: List<OmniPoolTokenId>): List<RemoteAndLocalId> {
        val runtime = chainRegistry.getRuntime(chain.id)
        val omniPoolTokenIds = chain.assets.associateBy(
            keySelector = { it.omniPoolTokenIdOrNull(runtime) },
            valueTransform = { it.fullId }
        )

        return onChainIds.mapNotNull { onChainId ->
            val id = omniPoolTokenIds[onChainId] ?: return@mapNotNull null

            onChainId to id
        }
    }

    private fun Chain.Asset.omniPoolTokenIdOrNull(runtimeSnapshot: RuntimeSnapshot): OmniPoolTokenId? {
        return when (val type = type) {
            is Chain.Asset.Type.Orml -> bindNumberOrNull(type.decodeOrNull(runtimeSnapshot))
            is Chain.Asset.Type.Native -> SYSTEM_ON_CHAIN_ASSET_ID
            else -> null
        }
    }

    private fun Chain.Asset.requireOmniPoolTokenId(runtimeSnapshot: RuntimeSnapshot): OmniPoolTokenId {
        return requireNotNull(omniPoolTokenIdOrNull(runtimeSnapshot))
    }

    private fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        val assetIdIn = args.assetIn.requireOmniPoolTokenId(runtime)
        val assetIdOut = args.assetOut.requireOmniPoolTokenId(runtime)

        when(val limit = args.swapLimit) {
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

    private fun ExtrinsicBuilder.sell(
        assetIdIn: OmniPoolTokenId,
        assetIdOut: OmniPoolTokenId,
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
        assetIdIn: OmniPoolTokenId,
        assetIdOut: OmniPoolTokenId,
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
}

private typealias RemoteAndLocalId = Pair<OmniPoolTokenId, FullChainAssetId>
