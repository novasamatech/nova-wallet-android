package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.dynamicFees
import io.novafoundation.nova.common.utils.flatMap
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
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxNovaReferral
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.acceptedCurrencies
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.accountCurrencyMap
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.multiTransactionPayment
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.DynamicFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPool
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolFees
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolToken
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolTokenId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmnipoolAssetState
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.feeParamsConstant
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.quote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.linkedAccounts
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.referralsOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSystemAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.toOnChainIdOrThrow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import java.math.BigInteger

class HydraDxOmnipoolExchangeFactory(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
) : AssetExchange.Factory {

    override suspend fun create(chain: Chain, coroutineScope: CoroutineScope): AssetExchange {
        return HydraDxOmnipoolExchange(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            chain = chain,
            storageSharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
            assetSourceRegistry = assetSourceRegistry,
            extrinsicService = extrinsicService,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydraDxNovaReferral = hydraDxNovaReferral
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
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
) : AssetExchange {

    private val pooledOnChainAssetIdsState: MutableSharedFlow<List<RemoteAndLocalId>> = singleReplaySharedFlow()

    private val omniPoolFlow: MutableSharedFlow<OmniPool> = singleReplaySharedFlow()

    private val currentPaymentAsset: MutableSharedFlow<OmniPoolTokenId> = singleReplaySharedFlow()

    private val userReferralState: MutableSharedFlow<ReferralState> = singleReplaySharedFlow()

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        val onChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(asset)

        if (hydraDxAssetIdConverter.isSystemAsset(onChainId)) return true

        val fallbackPrice = remoteStorageSource.query(chain.id) {
            metadata.multiTransactionPayment.acceptedCurrencies.query(onChainId)
        }

        return fallbackPrice != null
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
        val omniPool = omniPoolFlow.first()

        val omniPoolTokenIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.tokenIn.configuration)
        val omniPoolTokenIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(args.tokenOut.configuration)

        val quote = omniPool.quote(omniPoolTokenIdIn, omniPoolTokenIdOut, args.amount, args.swapDirection)
            ?: throw SwapQuoteException.NotEnoughLiquidity

        return AssetExchangeQuote(quote)
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee {
        val feeAsset = args.usedFeeAsset
        val paymentCurrencyToSet = getPaymentCurrencyToSetIfNeeded(feeAsset)

        val setCurrencyFee = if (paymentCurrencyToSet != null) {
            extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
                setFeeCurrency(paymentCurrencyToSet)
            }
        } else {
            null
        }

        val swapFee = extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            executeSwap(args)
        }

        val totalNativeFee = swapFee.amount + setCurrencyFee?.amount.orZero()

        val feeAmountInExpectedCurrency = if (!feeAsset.isUtilityAsset) {
            convertNativeFeeToAssetFee(totalNativeFee, feeAsset)
        } else {
            totalNativeFee
        }
        val feeInExpectedCurrency = SubstrateFee(
            amount = feeAmountInExpectedCurrency,
            submissionOrigin = swapFee.submissionOrigin
        )

        return AssetExchangeFee(networkFee = feeInExpectedCurrency, MinimumBalanceBuyIn.NoBuyInNeeded)
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        val feeAsset = args.usedFeeAsset
        val paymentCurrencyToSet = getPaymentCurrencyToSetIfNeeded(feeAsset)

        val setCurrencyResult = if (paymentCurrencyToSet != null) {
            extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                setFeeCurrency(paymentCurrencyToSet)
            }.awaitInBlock() // we need to wait for tx execution for currency update changes to be taken into account by runtime with executing swap itself
        } else {
            Result.success(Unit)
        }

        return setCurrencyResult.flatMap {
            extrinsicService.submitExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                executeSwap(args)
            }
        }
    }

    override suspend fun slippageConfig(): SlippageConfig {
        return SlippageConfig.default()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun runSubscriptions(chain: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        omniPoolFlow.resetReplayCache()

        return withFlowScope { scope ->
            val pooledAssets = pooledOnChainAssetIdsState.first()
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)

            val omniPoolStateFlow = pooledAssets.map { (onChainId, _) ->
                remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                    metadata.omnipool.assets.observeNonNull(onChainId).map {
                        onChainId to it
                    }
                }
            }
                .toMultiSubscription(pooledAssets.size)

            val poolAccountId = poolAccountId()

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

            val userAccountId = metaAccount.requireAccountIdIn(chain)

            val feeCurrency = remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                metadata.multiTransactionPayment.accountCurrencyMap.observe(userAccountId)
            }

            val userReferral = subscribeUserReferral(userAccountId, subscriptionBuilder).onEach {
                userReferralState.emit(it)
            }

            subscriptionBuilder.subscribe(scope)

            val poolStateUpdates = combine(omniPoolStateFlow, omniPoolBalancesFlow, feesFlow) { poolState, poolBalances, fees ->
                createOmniPool(poolState, poolBalances, fees, defaultFees)
            }
                .onEach(omniPoolFlow::emit)

            val feeCurrencyUpdates = feeCurrency.onEach { tokenId ->
                val feePaymentAsset = tokenId ?: hydraDxAssetIdConverter.systemAssetId
                currentPaymentAsset.emit(feePaymentAsset)
            }

            combine(poolStateUpdates, feeCurrencyUpdates, userReferral) { _, _, _ ->
                ReQuoteTrigger
            }
        }
    }

    private val SwapExecuteArgs.usedFeeAsset: Chain.Asset
        get() = customFeeAsset ?: chain.utilityAsset

    @Suppress("IfThenToElvis")
    private suspend fun subscribeUserReferral(
        userAccountId: AccountId,
        subscriptionBuilder: StorageSharedRequestsBuilder
    ): Flow<ReferralState> {
        return remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
            val referralsModule = metadata.referralsOrNull

            if (referralsModule != null) {
                referralsModule.linkedAccounts.observe(userAccountId).map { linkedAccount ->
                    if (linkedAccount != null) ReferralState.SET else ReferralState.NOT_SET
                }
            } else {
                flowOf(ReferralState.NOT_AVAILABLE)
            }
        }
    }

    // This will most probably slightly over-estimate the fee since Hydra runtime uses not only Omni-pool to convert native fee
    private suspend fun convertNativeFeeToAssetFee(
        nativeFeeAmount: Balance,
        targetAsset: Chain.Asset
    ): Balance {
        val omniPool = omniPoolFlow.first()

        val omniPoolTokenIdIn = hydraDxAssetIdConverter.toOnChainIdOrThrow(targetAsset)
        val omniPoolTokenIdOut = hydraDxAssetIdConverter.toOnChainIdOrThrow(chain.utilityAsset)

        val targetAssetAmount = omniPool.quote(
            assetIdIn = omniPoolTokenIdIn,
            assetIdOut = omniPoolTokenIdOut,
            amount = nativeFeeAmount,
            direction = SwapDirection.SPECIFIED_OUT
        )

        return requireNotNull(targetAssetAmount) // we don't expect liquidity run out for fee conversion
    }

    private suspend fun getPaymentCurrencyToSetIfNeeded(expectedPaymentAsset: Chain.Asset): OmniPoolTokenId? {
        val currencyPaymentTokenId = currentPaymentAsset.first()
        val expectedPaymentTokenId = hydraDxAssetIdConverter.toOnChainIdOrThrow(expectedPaymentAsset)

        return expectedPaymentTokenId.takeIf { currencyPaymentTokenId != expectedPaymentTokenId }
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

    private fun createOmniPool(
        poolAssetStates: Map<OmniPoolTokenId, OmnipoolAssetState>,
        poolBalances: Map<OmniPoolTokenId, Balance>,
        fees: Map<OmniPoolTokenId, DynamicFee?>,
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
        val omniPoolTokenIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return onChainIds.mapNotNull { onChainId ->
            val asset = omniPoolTokenIds[onChainId] ?: return@mapNotNull null

            onChainId to asset.fullId
        }
    }

    private suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        maybeSetReferral()

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

    private suspend fun ExtrinsicBuilder.maybeSetReferral() {
        val referralState = userReferralState.first()

        if (referralState == ReferralState.NOT_SET) {
            val novaReferralCode = hydraDxNovaReferral.getNovaReferralCode()

            linkCode(novaReferralCode)
        }
    }

    private fun ExtrinsicBuilder.linkCode(referralCode: String) {
        call(
            moduleName = Modules.REFERRALS,
            callName = "link_code",
            arguments = mapOf(
                "code" to referralCode.encodeToByteArray()
            )
        )
    }

    private fun ExtrinsicBuilder.setFeeCurrency(onChainId: OmniPoolTokenId) {
        call(
            moduleName = Modules.MULTI_TRANSACTION_PAYMENT,
            callName = "set_currency",
            arguments = mapOf(
                "currency" to onChainId
            )
        )
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

    private enum class ReferralState {
        SET, NOT_SET, NOT_AVAILABLE
    }
}

private typealias RemoteAndLocalId = Pair<OmniPoolTokenId, FullChainAssetId>