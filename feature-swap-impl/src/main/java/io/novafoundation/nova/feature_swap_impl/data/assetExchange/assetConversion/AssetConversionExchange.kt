package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.assetConversionAssetIdType
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.assetConversionOrNull
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.pools
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOutcomeOk
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.NovaFeeChargingSwapEdge
import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee.SwapSegment.SegmentNetFlow
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmissionResult
import io.novafoundation.nova.feature_swap_api.domain.model.UsdConverter
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.fee.SubmissionOnlyAtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.ParentQuoterArgs
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.domain.swap.BaseSwapGraphEdge
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.converter.toMultiLocationOrThrow
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_api.versions.orDefault
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEventOrThrow
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration

class AssetConversionExchangeFactory(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val remoteStorageSource: StorageDataSource,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi,
    private val chainStateRepository: ChainStateRepository,
    private val deductionUseCase: AssetInAdditionalSwapDeductionUseCase,
    private val xcmVersionDetector: XcmVersionDetector,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountInfoRepository: AccountInfoRepository,
    private val novaSwapCommission: NovaSwapCommission,
    private val commissionResolver: AssetHubCommissionResolver,
) : AssetExchange.SingleChainFactory {

    override suspend fun create(
        chain: Chain,
        swapHost: AssetExchange.SwapHost,
    ): AssetExchange {
        val converter = multiLocationConverterFactory.defaultAsync(chain, swapHost.scope)

        return AssetConversionExchange(
            chain = chain,
            multiLocationConverter = converter,
            remoteStorageSource = remoteStorageSource,
            multiChainRuntimeCallsApi = runtimeCallsApi,
            chainStateRepository = chainStateRepository,
            swapHost = swapHost,
            deductionUseCase = deductionUseCase,
            xcmVersionDetector = xcmVersionDetector,
            assetSourceRegistry = assetSourceRegistry,
            accountInfoRepository = accountInfoRepository,
            novaSwapCommission = novaSwapCommission,
            commissionResolver = commissionResolver,
        )
    }
}

private class AssetConversionExchange(
    private val chain: Chain,
    private val multiLocationConverter: MultiLocationConverter,
    private val remoteStorageSource: StorageDataSource,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val chainStateRepository: ChainStateRepository,
    private val swapHost: AssetExchange.SwapHost,
    private val deductionUseCase: AssetInAdditionalSwapDeductionUseCase,
    private val xcmVersionDetector: XcmVersionDetector,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountInfoRepository: AccountInfoRepository,
    private val novaSwapCommission: NovaSwapCommission,
    private val commissionResolver: AssetHubCommissionResolver,
) : AssetExchange {

    private val commissionRecipient = novaSwapCommission.assetHubFeeAccountId(chain.id)

    override suspend fun sync() {
        // nothing to sync
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        return remoteStorageSource.query(chain.id) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAllAvailableDirections(allPools)
        }
    }

    override fun feePaymentOverrides(): List<FeePaymentProviderOverride> {
        return emptyList()
    }

    override fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return chainStateRepository.currentBlockNumberFlow(chain.id)
            .drop(1) // skip immediate value from the cache to not perform double-quote on chain change
            .map { ReQuoteTrigger }
    }

    private suspend fun constructAllAvailableDirections(pools: List<Pair<RelativeMultiLocation, RelativeMultiLocation>>): List<AssetConversionEdge> {
        return buildList {
            pools.forEach { (firstLocation, secondLocation) ->
                val firstAsset = multiLocationConverter.toChainAsset(firstLocation) ?: return@forEach
                val secondAsset = multiLocationConverter.toChainAsset(secondLocation) ?: return@forEach

                add(assetConversionEdge(firstAsset, secondAsset))
                add(assetConversionEdge(secondAsset, firstAsset))
            }
        }
    }

    private fun assetConversionEdge(fromAsset: Chain.Asset, toAsset: Chain.Asset): AssetConversionEdge {
        return if (commissionRecipient != null) {
            CommissionedAssetConversionEdge(fromAsset, toAsset)
        } else {
            AssetConversionEdge(fromAsset, toAsset)
        }
    }

    private suspend fun detectAssetIdXcmVersion(runtime: RuntimeSnapshot): XcmVersion {
        val assetIdType = runtime.metadata.assetConversionAssetIdType()
        return xcmVersionDetector.detectMultiLocationVersion(chain.id, assetIdType).orDefault()
    }

    private suspend fun RuntimeCallsApi.quote(
        swapDirection: SwapDirection,
        assetIn: Chain.Asset,
        assetOut: Chain.Asset,
        amount: Balance,
    ): Balance? {
        val method = when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> "quote_price_exact_tokens_for_tokens"
            SwapDirection.SPECIFIED_OUT -> "quote_price_tokens_for_exact_tokens"
        }

        val assetIdXcmVersion = detectAssetIdXcmVersion(runtime)

        val asset1 = multiLocationConverter.toMultiLocationOrThrow(assetIn).toEncodableInstance(assetIdXcmVersion)
        val asset2 = multiLocationConverter.toMultiLocationOrThrow(assetOut).toEncodableInstance(assetIdXcmVersion)

        return call(
            section = "AssetConversionApi",
            method = method,
            arguments = mapOf(
                "asset1" to asset1,
                "asset2" to asset2,
                "amount" to amount,
                "include_fee" to true
            ),
            returnBinding = ::bindNumberOrNull
        )
    }

    private open inner class AssetConversionEdge(
        fromAsset: Chain.Asset,
        toAsset: Chain.Asset,
        private val chargesNovaCommission: Boolean = false,
    ) : BaseSwapGraphEdge(fromAsset, toAsset) {

        override suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation {
            return AssetConversionOperation(args, fromAsset, toAsset, chargesNovaCommission)
        }

        override suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation? {
            return null
        }

        override suspend fun beginOperationPrototype(): AtomicSwapOperationPrototype {
            return AssetConversionOperationPrototype(fromAsset.chainId, chargesNovaCommission)
        }

        override suspend fun appendToOperationPrototype(currentTransaction: AtomicSwapOperationPrototype): AtomicSwapOperationPrototype? {
            return null
        }

        override suspend fun debugLabel(): String {
            return "AssetConversion"
        }

        override fun predecessorHandlesFees(predecessor: SwapGraphEdge): Boolean {
            return false
        }

        override suspend fun canPayNonNativeFeesInIntermediatePosition(): Boolean {
            return true
        }

        override suspend fun canTransferOutWholeAccountBalance(): Boolean {
            return true
        }

        override suspend fun quote(
            amount: Balance,
            direction: SwapDirection
        ): Balance {
            val runtimeCallsApi = multiChainRuntimeCallsApi.forChain(chain.id)

            return runtimeCallsApi.quote(
                swapDirection = direction,
                assetIn = fromAsset,
                assetOut = toAsset,
                amount = amount
            ) ?: throw SwapQuoteException.NotEnoughLiquidity
        }

        override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>): Int {
            return Weights.AssetConversion.SWAP
        }
    }

    private inner class CommissionedAssetConversionEdge(fromAsset: Chain.Asset, toAsset: Chain.Asset) :
        AssetConversionEdge(fromAsset, toAsset, chargesNovaCommission = true),
        NovaFeeChargingSwapEdge

    inner class AssetConversionOperationPrototype(
        override val fromChain: ChainId,
        override val chargesServiceFee: Boolean,
    ) : AtomicSwapOperationPrototype {

        override suspend fun roughlyEstimateNativeFee(usdConverter: UsdConverter): BigDecimal {
            // in DOT
            return 0.0015.toBigDecimal()
        }

        override suspend fun maximumExecutionTime(): Duration {
            return chainStateRepository.expectedBlockTime(chain.id)
        }

        override fun serviceCommissionToAddOnTop(net: Balance): Balance {
            return novaSwapCommission.commissionToAddOnTop(net)
        }

        override fun serviceCommissionIncludedIn(gross: Balance): Balance {
            return novaSwapCommission.commissionIncludedIn(gross)
        }
    }

    inner class AssetConversionOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val fromAsset: Chain.Asset,
        private val toAsset: Chain.Asset,
        override val chargesServiceCommission: Boolean,
    ) : AtomicSwapOperation {

        override val estimatedSwapLimit: SwapLimit = transactionArgs.estimatedSwapLimit

        override val assetOut: FullChainAssetId = toAsset.fullId

        override val assetIn: FullChainAssetId = fromAsset.fullId

        override suspend fun constructDisplayData(netFlow: SegmentNetFlow): AtomicOperationDisplayData {
            return AtomicOperationDisplayData.Swap(
                from = fromAsset.fullId.withAmount(netFlow.amountIn),
                to = toAsset.fullId.withAmount(netFlow.amountOut),
            )
        }

        override suspend fun estimateFee(isServiceCommissionOperation: Boolean): AtomicSwapOperationFee {
            val preparedCommission = prepareCommissionIfNeeded(
                isServiceCommissionOperation = isServiceCommissionOperation,
                swapLimit = estimatedSwapLimit,
                prepare = ::prepareRequiredCommission,
            )

            val submissionFee = swapHost.extrinsicService().estimateFee(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    batchMode = BatchMode.BATCH_ALL,
                    feePaymentCurrency = transactionArgs.feePaymentCurrency,
                )
            ) {
                appendSwapCalls(
                    swapLimit = estimatedSwapLimit,
                    sendTo = chain.emptyAccountId(),
                    commission = preparedCommission,
                )
            }

            return SubmissionOnlyAtomicSwapOperationFee(
                submissionFee = submissionFee,
                serviceCommission = preparedCommission?.asFee(),
            )
        }

        override suspend fun requiredAmountInToGetAmountOut(extraOutAmount: Balance): Balance {
            val quoteArgs = ParentQuoterArgs(
                chainAssetIn = fromAsset,
                chainAssetOut = toAsset,
                amount = extraOutAmount,
                swapDirection = SwapDirection.SPECIFIED_OUT
            )

            return swapHost.quote(quoteArgs)
        }

        override suspend fun additionalMaxAmountDeduction(): SwapMaxAdditionalAmountDeduction {
            return SwapMaxAdditionalAmountDeduction(
                fromCountedTowardsEd = deductionUseCase.invoke(fromAsset, toAsset)
            )
        }

        override suspend fun execute(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection> {
            val preparedCommission = try {
                prepareCommissionIfNeeded(
                    isServiceCommissionOperation = args.isServiceCommissionOperation,
                    swapLimit = args.actualSwapLimit,
                    prepare = ::prepareCommissionOrWaive,
                )
            } catch (exception: Exception) {
                return Result.failure(exception)
            }

            return submitInternal(args, preparedCommission)
                .mapCatching {
                    val grossReceived = it.requireOutcomeOk().emittedEvents.determineActualSwappedAmount()
                    val netReceived = if (preparedCommission != null) {
                        (grossReceived - preparedCommission.amount).max(BigInteger.ZERO)
                    } else {
                        grossReceived
                    }
                    SwapExecutionCorrection(
                        actualReceivedAmount = netReceived
                    )
                }
        }

        override suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapSubmissionResult> {
            val preparedCommission = try {
                prepareCommissionIfNeeded(
                    isServiceCommissionOperation = args.isServiceCommissionOperation,
                    swapLimit = args.actualSwapLimit,
                    prepare = ::prepareRequiredCommission,
                )
            } catch (exception: Exception) {
                return Result.failure(exception)
            }

            return submitInternal(args, preparedCommission)
                .map { SwapSubmissionResult(it.submissionHierarchy) }
        }

        private suspend fun submitInternal(
            args: AtomicSwapOperationSubmissionArgs,
            commission: PreparedCommission?,
        ): Result<ExtrinsicExecutionResult> {
            return swapHost.extrinsicService().submitExtrinsicAndAwaitExecution(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    batchMode = BatchMode.BATCH_ALL,
                    feePaymentCurrency = transactionArgs.feePaymentCurrency,
                )
            ) { buildingContext ->
                // Send swapped funds to the executingAccount since it the account doing the swap
                appendSwapCalls(
                    swapLimit = args.actualSwapLimit,
                    sendTo = buildingContext.submissionOrigin.executingAccount,
                    commission = commission,
                )
            }.requireOk()
        }

        private suspend fun ExtrinsicBuilder.appendSwapCalls(
            swapLimit: SwapLimit,
            sendTo: AccountId,
            commission: PreparedCommission?,
        ) {
            executeSwap(swapLimit, sendTo)
            if (commission != null) {
                appendNovaCommissionCall(commission)
            }
        }

        private suspend fun prepareRequiredCommission(swapLimit: SwapLimit): PreparedCommission? {
            return prepareCommission(swapLimit) { minimumBalance ->
                commissionResolver.resolveRequired(swapLimit, minimumBalance)
            }
        }

        private suspend fun prepareCommissionIfNeeded(
            isServiceCommissionOperation: Boolean,
            swapLimit: SwapLimit,
            prepare: suspend (SwapLimit) -> PreparedCommission?,
        ): PreparedCommission? {
            return if (isServiceCommissionOperation) prepare(swapLimit) else null
        }

        private suspend fun prepareCommissionOrWaive(swapLimit: SwapLimit): PreparedCommission? {
            return prepareCommission(swapLimit) { minimumBalance ->
                commissionResolver.resolveOrWaive(swapLimit, minimumBalance)
            }
        }

        private suspend fun prepareCommission(
            swapLimit: SwapLimit,
            resolveAmount: (minimumBalance: Balance) -> Balance?,
        ): PreparedCommission? {
            val recipient = commissionRecipient ?: return null
            val outputBalance = assetSourceRegistry.sourceFor(toAsset).balance
            val minimumBalance = outputBalance.existentialDeposit(toAsset)
            val amount = resolveAmount(minimumBalance) ?: return null

            ensureCommissionRecipientReady(recipient)

            return PreparedCommission(recipient, amount)
        }

        private suspend fun ensureCommissionRecipientReady(recipient: AccountId) {
            val utilityAsset = chain.utilityAsset
            val utilityBalance = assetSourceRegistry.sourceFor(utilityAsset).balance
            val utilityMinimumBalance = utilityBalance.existentialDeposit(utilityAsset)
            val accountInfo = accountInfoRepository.getAccountInfo(chain.id, recipient)

            val systemAccountReady = accountInfo.providers > BigInteger.ZERO &&
                accountInfo.data.free >= utilityMinimumBalance
            val assetAccountReady = assetSourceRegistry.sourceFor(toAsset).balance.canReceive(chain, toAsset, recipient)

            if (!systemAccountReady || !assetAccountReady) {
                error("Asset Hub commission recipient cannot receive the asset")
            }
        }

        private suspend fun ExtrinsicBuilder.appendNovaCommissionCall(commission: PreparedCommission) {
            val transferBase = AssetTransferBase(
                recipient = chain.addressOf(commission.recipient),
                originChain = chain,
                originChainAsset = toAsset,
                destinationChain = chain,
                destinationChainAsset = toAsset,
                feePaymentCurrency = FeePaymentCurrency.Native,
                amountPlanks = commission.amount,
            )
            val commissionCall: GenericCall.Instance = assetSourceRegistry.sourceFor(toAsset).transfers
                .constructTransferCall(transferBase)
            call(commissionCall)
        }

        private inner class PreparedCommission(
            val recipient: AccountId,
            val amount: Balance,
        ) {

            fun asFee(): FeeBase = SubstrateFeeBase(amount, toAsset)
        }

        private fun List<GenericEvent.Instance>.determineActualSwappedAmount(): Balance {
            val swap = findEventOrThrow(Modules.ASSET_CONVERSION, "SwapExecuted")
            val (_, _, _, amountOut) = swap.arguments

            return bindNumber(amountOut)
        }

        private suspend fun ExtrinsicBuilder.executeSwap(
            swapLimit: SwapLimit,
            sendTo: AccountId
        ) {
            val assetIdXcmVersion = detectAssetIdXcmVersion(runtime)

            val path = listOf(fromAsset, toAsset)
                .map { asset -> multiLocationConverter.encodableMultiLocationOf(asset, assetIdXcmVersion) }

            val keepAlive = false

            when (swapLimit) {
                is SwapLimit.SpecifiedIn -> call(
                    moduleName = Modules.ASSET_CONVERSION,
                    callName = "swap_exact_tokens_for_tokens",
                    arguments = mapOf(
                        "path" to path,
                        "amount_in" to swapLimit.amountIn,
                        "amount_out_min" to swapLimit.amountOutMin,
                        "send_to" to sendTo,
                        "keep_alive" to keepAlive
                    )
                )

                is SwapLimit.SpecifiedOut -> call(
                    moduleName = Modules.ASSET_CONVERSION,
                    callName = "swap_tokens_for_exact_tokens",
                    arguments = mapOf(
                        "path" to path,
                        "amount_out" to swapLimit.amountOut,
                        "amount_in_max" to swapLimit.amountInMax,
                        "send_to" to sendTo,
                        "keep_alive" to keepAlive
                    )
                )
            }
        }

        private suspend fun MultiLocationConverter.encodableMultiLocationOf(
            chainAsset: Chain.Asset,
            xcmVersion: XcmVersion
        ): Any {
            return toMultiLocationOrThrow(chainAsset).toEncodableInstance(xcmVersion)
        }
    }
}
