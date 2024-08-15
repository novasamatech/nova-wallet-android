package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.firstById
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.findAllPossibleDirections
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
import io.novafoundation.nova.feature_swap_core.domain.model.QuotePath
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetConversion
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeFee
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxAssetConversionFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxSwapEdge
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.OmniPoolConversionSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.linkedAccounts
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.referralsOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_swap_core.data.network.isSystemAsset
import io.novafoundation.nova.feature_swap_core.data.network.setFeeCurrency
import io.novafoundation.nova.feature_swap_core.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.HydraDxNovaReferral
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.fullId
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val assetConversionFactory: HydraDxAssetConversionFactory
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
            assetSourceRegistry = assetSourceRegistry,
            assetConversion = assetConversionFactory.create(chain)
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
    private val assetConversion: AssetConversion<HydraDxSwapEdge>
) : AssetExchange {

    private val swapSources: List<HydraDxSwapSource> = createSources()

    private val currentPaymentAsset: MutableSharedFlow<HydraDxAssetId> = singleReplaySharedFlow()

    private val userReferralState: MutableSharedFlow<ReferralState> = singleReplaySharedFlow()

    private val quotePathsCache: MutableStateFlow<Map<QuotePathsCacheKey, QuotePathsCache>?> = MutableStateFlow(null)

    private val graphState: MutableSharedFlow<HydraSwapGraph> = singleReplaySharedFlow()

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        val onChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(asset)

        if (hydraDxAssetIdConverter.isSystemAsset(onChainId)) return true

        val fallbackPrice = remoteStorageSource.query(chain.id) {
            metadata.multiTransactionPayment.acceptedCurrencies.query(onChainId)
        }

        return fallbackPrice != null
    }

    override suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId> {
        val graph = assetConversion.availableSwapDirections()

        graphState.emit(graph)

        return graph.findAllPossibleDirections()
    }

    override suspend fun quote(args: AssetExchangeQuoteArgs): AssetExchangeQuote {
        val from = args.chainAssetIn.fullId
        val to = args.chainAssetOut.fullId

        val paths = pathsFromCacheOrCompute(from, to) {
            val graph = graphState.first()

            assetConversion.getPaths(graph, args)
        }

        return assetConversion.quote(paths, args)
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee {
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

        val swapFee = extrinsicService.estimateFee(
            chain,
            TransactionOrigin.SelectedWallet,
            submissionOptions = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.FORCE_BATCH)
        ) {
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
            submissionOrigin = swapFee.submissionOrigin,
            asset = expectedFeeAsset
        )

        return AssetExchangeFee(networkFee = feeInExpectedCurrency, MinimumBalanceBuyIn.NoBuyInNeeded)
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
            extrinsicService.submitExtrinsic(
                chain,
                TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.FORCE_BATCH)
            ) {
                executeSwap(args, paymentCurrencyToSet, currentFeeTokenId)
            }
        }
    }

    override suspend fun slippageConfig(): SlippageConfig {
        return SlippageConfig.default()
    }

    override fun runSubscriptions(chain: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return withFlowScope { scope ->
            assetConversion.sync()

            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)
            val userAccountId = metaAccount.requireAccountIdIn(chain)

            val feeCurrency = remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                metadata.multiTransactionPayment.accountCurrencyMap.observe(userAccountId)
            }

            val userReferral = subscribeUserReferral(userAccountId, subscriptionBuilder).onEach {
                userReferralState.emit(it)
            }

            val sourcesSubscription = assetConversion.runSubscriptions(userAccountId, subscriptionBuilder)

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

    private suspend fun ExtrinsicBuilder.executeSwap(
        args: SwapExecuteArgs,
        justSetFeeCurrency: HydraDxAssetId?,
        previousFeeCurrency: HydraDxAssetId
    ) {
        maybeSetReferral()

        addSwapCall(args)

        setFeeCurrencyToNative(justSetFeeCurrency, previousFeeCurrency)
    }

    private suspend fun ExtrinsicBuilder.addSwapCall(args: SwapExecuteArgs) {
        val sourceForOptimizedTrade = args.path.checkForOptimizedTrade()

        if (sourceForOptimizedTrade != null) {
            with(sourceForOptimizedTrade) {
                executeSwap(args)
            }
        } else {
            executeRouterSwap(args)
        }
    }

    private fun ExtrinsicBuilder.setFeeCurrencyToNative(justSetFeeCurrency: HydraDxAssetId?, previousFeeCurrency: HydraDxAssetId) {
        val justSetFeeToNonNative = justSetFeeCurrency != null && justSetFeeCurrency != hydraDxAssetIdConverter.systemAssetId
        val previousCurrencyRemainsNonNative = justSetFeeCurrency == null && previousFeeCurrency != hydraDxAssetIdConverter.systemAssetId

        if (justSetFeeToNonNative || previousCurrencyRemainsNonNative) {
            setFeeCurrency(hydraDxAssetIdConverter.systemAssetId)
        }
    }

    private suspend fun ExtrinsicBuilder.executeRouterSwap(args: SwapExecuteArgs) {
        when (val limit = args.swapLimit) {
            is SwapLimit.SpecifiedIn -> executeRouterSell(args, limit)
            is SwapLimit.SpecifiedOut -> executeRouterBuy(args, limit)
        }
    }

    private suspend fun ExtrinsicBuilder.executeRouterBuy(args: SwapExecuteArgs, limit: SwapLimit.SpecifiedOut) {
        call(
            moduleName = Modules.ROUTER,
            callName = "buy",
            arguments = mapOf(
                "asset_in" to hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetIn),
                "asset_out" to hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetOut),
                "amount_out" to limit.expectedAmountOut,
                "max_amount_in" to limit.amountInMax,
                "route" to args.path.convertToRouterTrade()
            )
        )
    }

    private suspend fun ExtrinsicBuilder.executeRouterSell(args: SwapExecuteArgs, limit: SwapLimit.SpecifiedIn) {
        call(
            moduleName = Modules.ROUTER,
            callName = "sell",
            arguments = mapOf(
                "asset_in" to hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetIn),
                "asset_out" to hydraDxAssetIdConverter.toOnChainIdOrThrow(args.assetOut),
                "amount_in" to limit.expectedAmountIn,
                "min_amount_out" to limit.amountOutMin,
                "route" to args.path.convertToRouterTrade()
            )
        )
    }

    private suspend fun QuotePath.convertToRouterTrade(): List<Any?> {
        return segments.map { segment ->
            val source = swapSources.firstById(segment.sourceId)

            structOf(
                "pool" to source.routerPoolTypeFor(segment.sourceParams),
                "assetIn" to hydraDxAssetIdConverter.toOnChainIdOrThrow(segment.from),
                "assetOut" to hydraDxAssetIdConverter.toOnChainIdOrThrow(segment.to)
            )
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

    private inline fun pathsFromCacheOrCompute(
        from: FullChainAssetId,
        to: FullChainAssetId,
        computation: () -> List<Path<HydraDxSwapEdge>>
    ): List<Path<HydraDxSwapEdge>> {
        val mapKey = from to to
        val cachedMap = quotePathsCache.value.orEmpty()
        val cachedValue = cachedMap[mapKey]

        if (cachedValue != null) {
            return cachedValue.paths
        }

        val computedPaths = computation()

        val updatedMap = cachedMap + (mapKey to QuotePathsCache(computedPaths))
        quotePathsCache.value = updatedMap

        return computedPaths
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

    private fun QuotePath.checkForOptimizedTrade(): HydraDxSwapSource? {
        if (segments.size != 1) return null

        val onlySegment = segments.single()

        return if (onlySegment.canOptimizeSingleSegmentTrade()) {
            swapSources.findOmniPool()
        } else {
            null
        }
    }

    private fun QuotePath.Segment.canOptimizeSingleSegmentTrade(): Boolean {
        return sourceId == OmniPoolConversionSourceFactory.SOURCE_ID
    }

    private fun Iterable<HydraDxSwapSource>.findOmniPool(): HydraDxSwapSource {
        return firstById(OmniPoolConversionSourceFactory.SOURCE_ID)
    }

    private fun createSources(): List<HydraDxSwapSource> {
        return swapSourceFactories.map { it.create(chain) }
    }
}
