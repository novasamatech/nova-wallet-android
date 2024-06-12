package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.firstById
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.flatMapAsync
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.QuotableEdge
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.OmniPoolSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.linkedAccounts
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.referralsOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSystemAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.toOnChainIdOrThrow
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach


class HydraDxExchangeFactory(
    private val remoteStorageSource: StorageDataSource,
    private val sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val extrinsicService: ExtrinsicService,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
    private val swapSourceFactories: Iterable<HydraDxSwapSource.Factory>,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetExchange.Factory {

    override suspend fun create(chain: Chain, coroutineScope: CoroutineScope): AssetExchange {
        return HydraDxExchange(
            remoteStorageSource = remoteStorageSource,
            chain = chain,
            storageSharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
            extrinsicService = extrinsicService,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydraDxNovaReferral = hydraDxNovaReferral,
            swapSourceFactories = swapSourceFactories,
            assetSourceRegistry = assetSourceRegistry
        )
    }
}

private typealias HydraSwapGraph = Graph<FullChainAssetId, HydraDxSwapEdge>
private typealias QuotePathsCacheKey = Pair<FullChainAssetId, FullChainAssetId>

private class HydraDxExchange(
    private val remoteStorageSource: StorageDataSource,
    private val chain: Chain,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val extrinsicService: ExtrinsicService,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
    private val swapSourceFactories: Iterable<HydraDxSwapSource.Factory>,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetExchange {

    private val swapSources: List<HydraDxSwapSource> = createSources()

    private val currentPaymentAsset: MutableSharedFlow<HydraDxAssetId> = singleReplaySharedFlow()

    private val userReferralState: MutableSharedFlow<ReferralState> = singleReplaySharedFlow()

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        val onChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(asset)

        if (hydraDxAssetIdConverter.isSystemAsset(onChainId)) return true

        val fallbackPrice = remoteStorageSource.query(chain.id) {
            metadata.multiTransactionPayment.acceptedCurrencies.query(onChainId)
        }

        return fallbackPrice != null
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        return swapSources.flatMapAsync { source ->
            source.availableSwapDirections().map(::HydraDxSwapEdge)
        }
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AtomicSwapOperationFee {
        val expectedFeeAsset = args.usedFeeAsset

        val currentFeeTokenId = currentPaymentAsset.first()
        val paymentCurrencyToSet = getPaymentCurrencyToSetIfNeeded(expectedFeeAsset, currentFeeTokenId)

        val setCurrencyFee = if (paymentCurrencyToSet != null) {
            extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
                setFeeCurrency(paymentCurrencyToSet)
            }
        } else {
            null
        }

        val swapFee = extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet, BatchMode.FORCE_BATCH) {
            executeSwap(args, paymentCurrencyToSet, currentFeeTokenId)
        }

        val totalNativeFee = swapFee.amount + setCurrencyFee?.amount.orZero()

        val feeAmountInExpectedCurrency = if (!expectedFeeAsset.isUtilityAsset) {
            convertNativeFeeToAssetFee(totalNativeFee, expectedFeeAsset)
        } else {
            totalNativeFee
        }
        val feeInExpectedCurrency = SubstrateFee(
            amount = feeAmountInExpectedCurrency,
            submissionOrigin = swapFee.submissionOrigin
        )

        return AtomicSwapOperationFee(networkFee = feeInExpectedCurrency, MinimumBalanceBuyIn.NoBuyInNeeded)
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        val expectedFeeAsset = args.usedFeeAsset

        val currentFeeTokenId = currentPaymentAsset.first()
        val paymentCurrencyToSet = getPaymentCurrencyToSetIfNeeded(expectedFeeAsset, currentFeeTokenId)

        val setCurrencyResult = if (paymentCurrencyToSet != null) {
            extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                setFeeCurrency(paymentCurrencyToSet)
            }.awaitInBlock() // we need to wait for tx execution for currency update changes to be taken into account by runtime with executing swap itself
        } else {
            Result.success(Unit)
        }

        return setCurrencyResult.flatMap {
            extrinsicService.submitExtrinsic(chain, TransactionOrigin.SelectedWallet, BatchMode.FORCE_BATCH) {
                executeSwap(args, paymentCurrencyToSet, currentFeeTokenId)
            }
        }
    }

    override suspend fun slippageConfig(): SlippageConfig {
        return SlippageConfig.default()
    }

    override fun runSubscriptions(chain: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return withFlowScope { scope ->
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)
            val userAccountId = metaAccount.requireAccountIdIn(chain)

            val feeCurrency = remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                metadata.multiTransactionPayment.accountCurrencyMap.observe(userAccountId)
            }

            val userReferral = subscribeUserReferral(userAccountId, subscriptionBuilder).onEach {
                userReferralState.emit(it)
            }

            val sourcesSubscription = swapSources.map {
                it.runSubscriptions(userAccountId, subscriptionBuilder)
            }.mergeIfMultiple()

            subscriptionBuilder.subscribe(scope)

            val feeCurrencyUpdates = feeCurrency.onEach { tokenId ->
                val feePaymentAsset = tokenId ?: hydraDxAssetIdConverter.systemAssetId
                currentPaymentAsset.emit(feePaymentAsset)
            }

            combine(sourcesSubscription, feeCurrencyUpdates, userReferral) { _, _, _ ->
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

    private suspend fun convertNativeFeeToAssetFee(
        nativeFeeAmount: Balance,
        targetAsset: Chain.Asset
    ): Balance {
        val args = AssetExchangeQuoteArgs(
            chainAssetIn = targetAsset,
            chainAssetOut = chain.utilityAsset,
            amount = nativeFeeAmount,
            swapDirection = SwapDirection.SPECIFIED_OUT
        )

        val quotedFee = quote(args).quote

        // TODO
        // There is a issue in Router implementation in Hydra that doesn't allow asset balance to go below ED. We add it to fee for simplicity instead
        // of refactoring SwapExistentialDepositAwareMaxActionProvider
        // This should be removed once Router issue is fixed
        val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain, targetAsset)

        return quotedFee + existentialDeposit
    }

    private suspend fun getPaymentCurrencyToSetIfNeeded(expectedPaymentAsset: Chain.Asset, currentFeeTokenId: HydraDxAssetId): HydraDxAssetId? {
        val expectedPaymentTokenId = hydraDxAssetIdConverter.toOnChainIdOrThrow(expectedPaymentAsset)

        return expectedPaymentTokenId.takeIf { currentFeeTokenId != expectedPaymentTokenId }
    }


    private fun ExtrinsicBuilder.setFeeCurrencyToNative(justSetFeeCurrency: HydraDxAssetId?, previousFeeCurrency: HydraDxAssetId) {
        val justSetFeeToNonNative = justSetFeeCurrency != null && justSetFeeCurrency != hydraDxAssetIdConverter.systemAssetId
        val previousCurrencyRemainsNonNative = justSetFeeCurrency == null && previousFeeCurrency != hydraDxAssetIdConverter.systemAssetId

        if (justSetFeeToNonNative || previousCurrencyRemainsNonNative) {
            setFeeCurrency(hydraDxAssetIdConverter.systemAssetId)
        }
    }

    private suspend fun HydraDxAssetIdConverter.toOnChainIdOrThrow(localId: FullChainAssetId): HydraDxAssetId {
        val chainAsset = chain.assetsById.getValue(localId.assetId)

        return toOnChainIdOrThrow(chainAsset)
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

    private fun ExtrinsicBuilder.setFeeCurrency(onChainId: HydraDxAssetId) {
        call(
            moduleName = Modules.MULTI_TRANSACTION_PAYMENT,
            callName = "set_currency",
            arguments = mapOf(
                "currency" to onChainId
            )
        )
    }


    private enum class ReferralState {
        SET, NOT_SET, NOT_AVAILABLE
    }

    private class QuotePathsCache(
        val paths: List<Path<HydraDxSwapEdge>>
    )

    private fun HydraDxSwapEdge.swapSource(): HydraDxSwapSource {
        return swapSources.firstById(sourceId)
    }


    private fun Iterable<HydraDxSwapSource>.findOmniPool(): HydraDxSwapSource {
        return firstById(OmniPoolSwapSourceFactory.SOURCE_ID)
    }

    private fun createSources(): List<HydraDxSwapSource> {
        return swapSourceFactories.map { it.create(chain) }
    }

    private inner class HydraDxSwapEdge(
        private val sourceQuotableEdge: HydraDxSourceEdge,
    ) : SwapGraphEdge, QuotableEdge by sourceQuotableEdge {

        override suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation {
            return HydraDxOperation(HydraDxSwapTransactionSegment(sourceQuotableEdge, args))
        }

        override suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation? {
            if (currentTransaction !is HydraDxOperation) return null

            return currentTransaction.appendSegment(HydraDxSwapTransactionSegment(sourceQuotableEdge, args))
        }
    }

    inner class HydraDxOperation(
        private val segments: List<HydraDxSwapTransactionSegment>,
    ) : AtomicSwapOperation {

        constructor(segment: HydraDxSwapTransactionSegment) : this(listOf(segment))

        fun appendSegment(nextSegment: HydraDxSwapTransactionSegment): HydraDxOperation {
            return HydraDxOperation(segments + nextSegment)
        }

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            TODO("Not yet implemented")
        }

        override suspend fun submit(previousStepCorrection: SwapExecutionCorrection?): Result<SwapExecutionCorrection> {
            // TODO use `previousStepCorrection` to correct used call arguments

            TODO("Not yet implemented")
        }

        private suspend fun ExtrinsicBuilder.executeSwap(
            justSetFeeCurrency: HydraDxAssetId?,
            previousFeeCurrency: HydraDxAssetId
        ) {
            maybeSetReferral()

            addSwapCall()

            setFeeCurrencyToNative(justSetFeeCurrency, previousFeeCurrency)
        }

        private suspend fun ExtrinsicBuilder.addSwapCall() {
            val optimizationSucceeded = tryOptimizedSwap()

            if (!optimizationSucceeded) {
                executeRouterSwap()
            }
        }

        private fun ExtrinsicBuilder.tryOptimizedSwap(): Boolean {
            if (segments.size != 1) return false

            val onlySegment = segments.single()
            val standaloneSwapBuilder = onlySegment.edge.standaloneSwapBuilder ?: return false

            standaloneSwapBuilder(onlySegment.segmentArgs)
            return true
        }

        private suspend fun ExtrinsicBuilder.executeRouterSwap() {
            val firstSegment = segments.first()
            val lastSegment = segments.last()

            when (val firstLimit = firstSegment.segmentArgs.swapLimit) {
                is SwapLimit.SpecifiedIn -> executeRouterSell(firstSegment.edge, firstLimit, lastSegment.edge, lastSegment.segmentArgs.swapLimit as SwapLimit.SpecifiedIn)
                is SwapLimit.SpecifiedOut -> executeRouterBuy(firstSegment.edge, firstLimit, lastSegment.edge, lastSegment.segmentArgs.swapLimit as SwapLimit.SpecifiedOut)
            }
        }

        private suspend fun ExtrinsicBuilder.executeRouterBuy(
            firstEdge: HydraDxSourceEdge,
            firstLimit: SwapLimit.SpecifiedOut,
            lastEdge: HydraDxSourceEdge,
            lastLimit: SwapLimit.SpecifiedOut
        ) {
            call(
                moduleName = Modules.ROUTER,
                callName = "buy",
                arguments = mapOf(
                    "asset_in" to hydraDxAssetIdConverter.toOnChainIdOrThrow(firstEdge.from),
                    "asset_out" to hydraDxAssetIdConverter.toOnChainIdOrThrow(lastEdge.to),
                    "amount_out" to lastLimit.amountOut,
                    "max_amount_in" to firstLimit.amountInMax,
                    "route" to routerTradePath()
                )
            )
        }

        private suspend fun ExtrinsicBuilder.executeRouterSell(
            firstEdge: HydraDxSourceEdge,
            firstLimit: SwapLimit.SpecifiedIn,
            lastEdge: HydraDxSourceEdge,
            lastLimit: SwapLimit.SpecifiedIn
        ) {
            call(
                moduleName = Modules.ROUTER,
                callName = "sell",
                arguments = mapOf(
                    "asset_in" to hydraDxAssetIdConverter.toOnChainIdOrThrow(firstEdge.from),
                    "asset_out" to hydraDxAssetIdConverter.toOnChainIdOrThrow(lastEdge.to),
                    "amount_in" to firstLimit.amountIn,
                    "min_amount_out" to lastLimit.amountOutMin,
                    "route" to routerTradePath()
                )
            )
        }

        private suspend fun routerTradePath(): List<Any?> {
            return segments.map { segment ->
                structOf(
                    "pool" to segment.edge.routerPoolArgument(),
                    "assetIn" to hydraDxAssetIdConverter.toOnChainIdOrThrow(segment.edge.from),
                    "assetOut" to hydraDxAssetIdConverter.toOnChainIdOrThrow(segment.edge.to)
                )
            }
        }
    }
}

private class HydraDxSwapTransactionSegment(
    val edge: HydraDxSourceEdge,
    val segmentArgs: AtomicSwapOperationArgs,
)

private class HydraDxSwapEdge(
    override val from: FullChainAssetId,
    val sourceId: HydraDxSwapSourceId,
    val direction: HydraSwapDirection
) : Edge<FullChainAssetId> {

    override val to: FullChainAssetId = direction.to
}
