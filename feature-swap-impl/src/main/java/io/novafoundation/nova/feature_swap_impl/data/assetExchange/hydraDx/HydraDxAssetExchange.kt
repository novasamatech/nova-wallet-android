package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.flatMapAsync
import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.times
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.chains.CustomOrNativeFeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.ResetMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetFeesMode
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector.SetMode
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.UsdConverter
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountIn
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountOut
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.fee.SubmissionOnlyAtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.acceptedCurrencies
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
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEventOrThrow
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
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
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration


class HydraDxExchangeFactory(
    private val remoteStorageSource: StorageDataSource,
    private val sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
    private val swapSourceFactories: Iterable<HydraDxSwapSource.Factory<*>>,
    private val quotingFactory: HydraDxQuoting.Factory,
    private val hydrationFeeInjector: HydrationFeeInjector,
    private val chainStateRepository: ChainStateRepository
) : AssetExchange.SingleChainFactory {

    override suspend fun create(chain: Chain, swapHost: AssetExchange.SwapHost, coroutineScope: CoroutineScope): AssetExchange {
        return HydraDxAssetExchange(
            remoteStorageSource = remoteStorageSource,
            chain = chain,
            storageSharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydraDxNovaReferral = hydraDxNovaReferral,
            swapSourceFactories = swapSourceFactories,
            swapHost = swapHost,
            hydrationFeeInjector = hydrationFeeInjector,
            delegate = quotingFactory.create(chain),
            chainStateRepository = chainStateRepository
        )
    }
}

private const val ROUTE_EXECUTED_AMOUNT_OUT_IDX = 3
private const val FEE_QUOTE_BUFFER = 1.1

private class HydraDxAssetExchange(
    private val delegate: HydraDxQuoting,
    private val remoteStorageSource: StorageDataSource,
    private val chain: Chain,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxNovaReferral: HydraDxNovaReferral,
    private val swapSourceFactories: Iterable<HydraDxSwapSource.Factory<*>>,
    private val swapHost: AssetExchange.SwapHost,
    private val hydrationFeeInjector: HydrationFeeInjector,
    private val chainStateRepository: ChainStateRepository
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

        override suspend fun beginOperationPrototype(): AtomicSwapOperationPrototype {
            return HydraDxOperationPrototype(from.chainId)
        }

        override suspend fun appendToOperationPrototype(currentTransaction: AtomicSwapOperationPrototype): AtomicSwapOperationPrototype? {
            return if (currentTransaction is HydraDxOperationPrototype) {
                currentTransaction
            } else {
                null
            }
        }

        override suspend fun debugLabel(): String {
            return sourceQuotableEdge.debugLabel()
        }

        override fun shouldIgnoreFeeRequirementAfter(predecessor: SwapGraphEdge): Boolean {
            // When chaining multiple hydra edges together, the fee is always paid with the starting edge
            return predecessor is HydraDxSwapEdge
        }

        override suspend fun canPayNonNativeFeesInIntermediatePosition(): Boolean {
            return true
        }
    }

    inner class HydraDxOperationPrototype(override val fromChain: ChainId) : AtomicSwapOperationPrototype {

        override suspend fun roughlyEstimateNativeFee(usdConverter: UsdConverter): BigDecimal {
            // in HDX
            return 0.5.toBigDecimal()
        }

        override suspend fun maximumExecutionTime(): Duration {
            return chainStateRepository.expectedBlockTime(chain.id)
        }
    }

    inner class HydraDxOperation private constructor(
        val segments: List<HydraDxSwapTransactionSegment>,
        val feePaymentCurrency: FeePaymentCurrency,
    ) : AtomicSwapOperation {

        override val estimatedSwapLimit: SwapLimit = aggregatedSwapLimit()
        constructor(sourceEdge: HydraDxSourceEdge, args: AtomicSwapOperationArgs)
            : this(listOf(HydraDxSwapTransactionSegment(sourceEdge, args.estimatedSwapLimit)), args.feePaymentCurrency)

        fun appendSegment(nextEdge: HydraDxSourceEdge, nextSwapArgs: AtomicSwapOperationArgs): HydraDxOperation {
            val nextSegment = HydraDxSwapTransactionSegment(nextEdge, nextSwapArgs.estimatedSwapLimit)

            // Ignore nextSwapArgs.feePaymentCurrency - we are using configuration from the very first segment
            return HydraDxOperation(segments + nextSegment, feePaymentCurrency)
        }

        override suspend fun constructDisplayData(): AtomicOperationDisplayData {
            return AtomicOperationDisplayData.Swap(
                from = segments.first().edge.from.withAmount(estimatedSwapLimit.estimatedAmountIn),
                to = segments.last().edge.to.withAmount(estimatedSwapLimit.estimatedAmountOut),
            )
        }

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val submissionFee = swapHost.extrinsicService().estimateFee(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    batchMode = BatchMode.BATCH_ALL,
                    feePaymentCurrency = feePaymentCurrency
                )
            ) {
                executeSwap(estimatedSwapLimit)
            }

            return SubmissionOnlyAtomicSwapOperationFee(submissionFee)
        }

        override suspend fun requiredAmountInToGetAmountOut(extraOutAmount: Balance): Balance {
            val assetInId = segments.first().edge.from.assetId
            val assetIn = chain.assetsById.getValue(assetInId)

            val assetOutId = segments.last().edge.to.assetId
            val assetOut = chain.assetsById.getValue(assetOutId)

            val quoteArgs = ParentQuoterArgs(
                chainAssetIn = assetIn,
                chainAssetOut = assetOut,
                amount = extraOutAmount,
                swapDirection = SwapDirection.SPECIFIED_OUT
            )

            return swapHost.quote(quoteArgs)
        }

        override suspend fun additionalMaxAmountDeduction(): Balance {
            return BigInteger.ZERO
        }

        override suspend fun inProgressLabel(): String {
            val assetInId = segments.first().edge.from.assetId
            val assetIn = chain.assetsById.getValue(assetInId)

            val assetOutId = segments.last().edge.to.assetId
            val assetOut = chain.assetsById.getValue(assetOutId)

            return "Swapping ${assetIn.symbol} to ${assetOut.symbol} on ${chain.name}"
        }

        override suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection> {
            return swapHost.extrinsicService().submitExtrinsicAndAwaitExecution(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    batchMode = BatchMode.BATCH_ALL,
                    feePaymentCurrency = feePaymentCurrency
                )
            ) {
                executeSwap(args.actualSwapLimit)
            }.requireOk().mapCatching { (events) ->
                SwapExecutionCorrection(
                    actualReceivedAmount = events.determineActualSwappedAmount()
                )
            }
        }

        private fun List<GenericEvent.Instance>.determineActualSwappedAmount(): Balance {
            val standaloneHydraSwap = getStandaloneSwap()
            if (standaloneHydraSwap != null) {
                return standaloneHydraSwap.extractReceivedAmount(this)
            }

            val swapExecutedEvent = findEvent(Modules.ROUTER, "RouteExecuted")
                ?: findEventOrThrow(Modules.ROUTER, "Executed")

            val amountOut = swapExecutedEvent.arguments[ROUTE_EXECUTED_AMOUNT_OUT_IDX]
            return bindNumber(amountOut)
        }

        private suspend fun ExtrinsicBuilder.executeSwap(actualSwapLimit: SwapLimit) {
            maybeSetReferral()

            addSwapCall(actualSwapLimit)
        }

        private suspend fun ExtrinsicBuilder.addSwapCall(actualSwapLimit: SwapLimit) {
            val optimizationSucceeded = tryOptimizedSwap(actualSwapLimit)

            if (!optimizationSucceeded) {
                executeRouterSwap(actualSwapLimit)
            }
        }

        private fun ExtrinsicBuilder.tryOptimizedSwap(actualSwapLimit: SwapLimit): Boolean {
            val standaloneSwap = getStandaloneSwap() ?: return false

            val args = AtomicSwapOperationArgs(actualSwapLimit, feePaymentCurrency)
            standaloneSwap.addSwapCall(args)

            return true
        }

        private fun getStandaloneSwap(): StandaloneHydraSwap? {
            if (segments.size != 1) return null

            val onlySegment = segments.single()
            return onlySegment.edge.standaloneSwap
        }

        private suspend fun ExtrinsicBuilder.executeRouterSwap(actualSwapLimit: SwapLimit) {
            val firstSegment = segments.first()
            val lastSegment = segments.last()

            when (actualSwapLimit) {
                is SwapLimit.SpecifiedIn -> executeRouterSell(
                    firstEdge = firstSegment.edge,
                    lastEdge = lastSegment.edge,
                    limit = actualSwapLimit,
                )

                is SwapLimit.SpecifiedOut -> executeRouterBuy(
                    firstEdge = firstSegment.edge,
                    lastEdge = lastSegment.edge,
                    limit = actualSwapLimit,
                )
            }
        }

        private suspend fun ExtrinsicBuilder.executeRouterBuy(
            firstEdge: HydraDxSourceEdge,
            lastEdge: HydraDxSourceEdge,
            limit: SwapLimit.SpecifiedOut,
        ) {
            call(
                moduleName = Modules.ROUTER,
                callName = "buy",
                arguments = mapOf(
                    "asset_in" to hydraDxAssetIdConverter.toOnChainIdOrThrow(firstEdge.from),
                    "asset_out" to hydraDxAssetIdConverter.toOnChainIdOrThrow(lastEdge.to),
                    "amount_out" to limit.amountOut,
                    "max_amount_in" to limit.amountInMax,
                    "route" to routerTradePath()
                )
            )
        }

        private suspend fun ExtrinsicBuilder.executeRouterSell(
            firstEdge: HydraDxSourceEdge,
            lastEdge: HydraDxSourceEdge,
            limit: SwapLimit.SpecifiedIn,
        ) {
            call(
                moduleName = Modules.ROUTER,
                callName = "sell",
                arguments = mapOf(
                    "asset_in" to hydraDxAssetIdConverter.toOnChainIdOrThrow(firstEdge.from),
                    "asset_out" to hydraDxAssetIdConverter.toOnChainIdOrThrow(lastEdge.to),
                    "amount_in" to limit.amountIn,
                    "min_amount_out" to limit.amountOutMin,
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

        private fun aggregatedSwapLimit(): SwapLimit {
            val firstSegment = segments.first()
            val lastSegment = segments.last()

            return when (val firstLimit = firstSegment.swapLimit) {
                is SwapLimit.SpecifiedIn -> {
                    val lastLimit = lastSegment.swapLimit as SwapLimit.SpecifiedIn

                    SwapLimit.SpecifiedIn(
                        amountIn = firstLimit.amountIn,
                        amountOutQuote = lastLimit.amountOutQuote,
                        amountOutMin = lastLimit.amountOutMin
                    )
                }

                is SwapLimit.SpecifiedOut -> {
                    val lastLimit = lastSegment.swapLimit as SwapLimit.SpecifiedOut

                    SwapLimit.SpecifiedOut(
                        amountOut = lastLimit.amountOut,
                        amountInQuote = firstLimit.amountInQuote,
                        amountInMax = firstLimit.amountInMax
                    )
                }
            }
        }
    }

    // This is an optimization to reuse swap quoting state for hydra fee estimation instead of letting ExtrinsicService to spin up its own quoting
    private inner class ReusableQuoteFeePaymentProvider : CustomOrNativeFeePaymentProvider() {

        override suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment {
            return ReusableQuoteFeePayment(customFeeAsset)
        }

        override suspend fun fastLookupCustomFeeCapability(): FastLookupCustomFeeCapability {
            val acceptedCurrencies = fetchAcceptedCurrencies()
            return HydrationFastLookupFeeCapability(acceptedCurrencies)
        }

        private suspend fun fetchAcceptedCurrencies(): Set<ChainAssetId> {
             val acceptedOnChainIds = remoteStorageSource.query(chain.id) {
                metadata.multiTransactionPayment.acceptedCurrencies.keys()
            }

            val onChainToLocalIds = hydraDxAssetIdConverter.allOnChainIds(chain)

            return acceptedOnChainIds.mapNotNullToSet { onChainToLocalIds[it]?.id }
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

            // Fees in non-native assets are especially volatile since conversion happens through swaps so we add some buffer to mitigate volatility
            val quotedFeeWithBuffer = quotedFee * FEE_QUOTE_BUFFER

            return SubstrateFee(
                amount = quotedFeeWithBuffer,
                submissionOrigin = nativeFee.submissionOrigin,
                asset = customFeeAsset
            )
        }

        override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
            return delegate.canPayFeeInNonUtilityToken(chainAsset)
        }
    }

    private inner class HydrationFastLookupFeeCapability(
        private val acceptedCurrencies: Set<ChainAssetId>
    ): FastLookupCustomFeeCapability {

        private var acceptedCurrenciesCache: Set<HydraDxAssetId>? = null

        override fun canPayFeeInNonUtilityToken(chainAssetId: ChainAssetId): Boolean {
            return chainAssetId in acceptedCurrencies
        }
    }

    class HydraDxSwapTransactionSegment(
        val edge: HydraDxSourceEdge,
        val swapLimit: SwapLimit,
    )
}
