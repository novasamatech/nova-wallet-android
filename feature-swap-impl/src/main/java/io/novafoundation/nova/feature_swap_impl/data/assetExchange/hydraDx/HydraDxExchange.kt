package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.flatMapAsync
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.chains.CustomOrNativeFeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.ResetMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetFeesMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetMode
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.accountCurrencyMap
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.multiTransactionPayment
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.ParentQuoterArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.HydraDxNovaReferral
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.linkedAccounts
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.referralsOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
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
    private val extrinsicServiceFactory: ExtrinsicService.Factory,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
    private val swapSourceFactories: Iterable<HydraDxSwapSource.Factory<*>>,
    private val quotingFactory: HydraDxQuoting.Factory,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val hydrationFeeInjector: HydrationFeeInjector
) : AssetExchange.SingleChainFactory {

    override suspend fun create(chain: Chain, parentQuoter: AssetExchange.SwapHost, coroutineScope: CoroutineScope): AssetExchange {
        return HydraDxExchange(
            remoteStorageSource = remoteStorageSource,
            chain = chain,
            storageSharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydraDxNovaReferral = hydraDxNovaReferral,
            swapSourceFactories = swapSourceFactories,
            assetSourceRegistry = assetSourceRegistry,
            swapHost = parentQuoter,
            hydrationFeeInjector = hydrationFeeInjector,
            delegate = quotingFactory.create(chain),
        )
    }
}

private class HydraDxExchange(
    private val delegate: HydraDxQuoting,
    private val remoteStorageSource: StorageDataSource,
    private val chain: Chain,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
    private val swapSourceFactories: Iterable<HydraDxSwapSource.Factory<*>>,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val swapHost: AssetExchange.SwapHost,
    private val hydrationFeeInjector: HydrationFeeInjector,
) : AssetExchange {

    private val swapSources: List<HydraDxSwapSource> = createSources()

    private val currentPaymentAsset: MutableSharedFlow<HydraDxAssetId> = singleReplaySharedFlow()

    private val userReferralState: MutableSharedFlow<ReferralState> = singleReplaySharedFlow()

    override suspend fun sync() {
        return swapSources.forEachAsync { it.sync() }
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        return swapSources.flatMapAsync { source ->
            source.availableSwapDirections().map(::HydraDxSwapEdge)
        }
    }

    override fun feePaymentOverrides(): List<FeePaymentProviderOverride> {
        return listOf(
            FeePaymentProviderOverride(
                provider = ReusableQuoteFeePaymentProvider(),
                chain = chain
            )
        )
    }

    override fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
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


    private suspend fun HydraDxAssetIdConverter.toOnChainIdOrThrow(localId: FullChainAssetId): HydraDxAssetId {
        val chainAsset = chain.assetsById.getValue(localId.assetId)

        return toOnChainIdOrThrow(chainAsset)
    }


    private enum class ReferralState {
        SET, NOT_SET, NOT_AVAILABLE
    }

    @Suppress("UNCHECKED_CAST")
    private fun createSources(): List<HydraDxSwapSource> {
        return swapSourceFactories.map {
            val sourceDelegate = delegate.getSource(it.identifier)

            // Cast should be safe as long as identifiers between delegates and wrappers match
            (it as HydraDxSwapSource.Factory<HydraDxQuotingSource<*>>).create(sourceDelegate)
        }
    }

    private inner class HydraDxSwapEdge(
        private val sourceQuotableEdge: HydraDxSourceEdge,
    ) : SwapGraphEdge, QuotableEdge by sourceQuotableEdge {

        override suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation {
            return HydraDxOperation(sourceQuotableEdge, args)
        }

        override suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation? {
            if (currentTransaction !is HydraDxOperation) return null

            return currentTransaction.appendSegment(sourceQuotableEdge, args)
        }

        override suspend fun debugLabel(): String {
            return "Hydration.${sourceQuotableEdge.debugLabel()}"
        }
    }

    inner class HydraDxOperation private constructor(
        val segments: List<HydraDxSwapTransactionSegment>,
        val feePaymentCurrency: FeePaymentCurrency,
    ) : AtomicSwapOperation {

        constructor(sourceEdge: HydraDxSourceEdge, args: AtomicSwapOperationArgs)
            : this(listOf(HydraDxSwapTransactionSegment(sourceEdge, args.swapLimit)), args.feePaymentCurrency)

        fun appendSegment(nextEdge: HydraDxSourceEdge, nextSwapArgs: AtomicSwapOperationArgs): HydraDxOperation {
            val nextSegment = HydraDxSwapTransactionSegment(nextEdge, nextSwapArgs.swapLimit)

            // Ignore nextSwapArgs.feePaymentCurrency - we are using configuration from the very first segment
            return HydraDxOperation(segments + nextSegment, feePaymentCurrency)
        }

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val submissionFee = swapHost.extrinsicService().estimateFee(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    batchMode = BatchMode.FORCE_BATCH,
                    feePaymentCurrency = feePaymentCurrency
                )
            ) {
                executeSwap()
            }

            return AtomicSwapOperationFee(submissionFee)
        }

        override suspend fun submit(previousStepCorrection: SwapExecutionCorrection?): Result<SwapExecutionCorrection> {
            return swapHost.extrinsicService().submitAndWatchExtrinsic(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    batchMode = BatchMode.FORCE_BATCH,
                    feePaymentCurrency = feePaymentCurrency
                )
            ) {
                executeSwap()
            }.awaitInBlock().map {
                SwapExecutionCorrection()
            }
        }

        private suspend fun ExtrinsicBuilder.executeSwap() {
            maybeSetReferral()

            addSwapCall()
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

            val args = AtomicSwapOperationArgs(onlySegment.swapLimit, feePaymentCurrency)
            standaloneSwapBuilder(args)

            return true
        }

        private suspend fun ExtrinsicBuilder.executeRouterSwap() {
            val firstSegment = segments.first()
            val lastSegment = segments.last()

            when (val firstLimit = firstSegment.swapLimit) {
                is SwapLimit.SpecifiedIn -> executeRouterSell(
                    firstEdge = firstSegment.edge,
                    firstLimit = firstLimit,
                    lastEdge = lastSegment.edge,
                    lastLimit = lastSegment.swapLimit as SwapLimit.SpecifiedIn
                )

                is SwapLimit.SpecifiedOut -> executeRouterBuy(
                    firstEdge = firstSegment.edge,
                    firstLimit = firstLimit,
                    lastEdge = lastSegment.edge,
                    lastLimit = lastSegment.swapLimit as SwapLimit.SpecifiedOut
                )
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
    }

    // This is an optimization to reuse swap quoting state for hydra fee estimation instead of letting ExtrinsicService to spin up its own quoting
    private inner class ReusableQuoteFeePaymentProvider : CustomOrNativeFeePaymentProvider() {

        override suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment {
            return ReusableQuoteFeePayment(customFeeAsset)
        }
    }

    private inner class ReusableQuoteFeePayment(
        private val customFeeAsset: Chain.Asset
    ) : FeePayment {

        override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
            val currentFeeTokenId = currentPaymentAsset.first()

            val setFeesMode = SetFeesMode(
                setMode = SetMode.Lazy(currentFeeTokenId),
                resetMode = ResetMode.ToNativeLazily(currentFeeTokenId)
            )

            hydrationFeeInjector.setFees(extrinsicBuilder, customFeeAsset, setFeesMode)
        }

        override suspend fun convertNativeFee(nativeFee: Fee): Fee {
            val args = ParentQuoterArgs(
                chainAssetIn = customFeeAsset,
                chainAssetOut = chain.utilityAsset,
                amount = nativeFee.amount,
                swapDirection = SwapDirection.SPECIFIED_OUT
            )

            val quotedFee = swapHost.quote(args)

            // TODO
            // There is a issue in Router implementation in Hydra that doesn't allow asset balance to go below ED. We add it to fee for simplicity instead
            // of refactoring SwapExistentialDepositAwareMaxActionProvider
            // This should be removed once Router issue is fixed
            val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain, customFeeAsset)
            val fee = quotedFee + existentialDeposit

            return SubstrateFee(
                amount = fee,
                submissionOrigin = nativeFee.submissionOrigin,
                asset = customFeeAsset
            )
        }

        override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
            return delegate.canPayFeeInNonUtilityToken(chainAsset)
        }
    }

    class HydraDxSwapTransactionSegment(
        val edge: HydraDxSourceEdge,
        val swapLimit: SwapLimit,
    )
}
