package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.assetConversionAssetIdType
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.assetConversionOrNull
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.pools
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.UsdConverter
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountIn
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountOut
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
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.converter.toMultiLocationOrThrow
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_api.versions.orDefault
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEventOrThrow
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOutcomeOk
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmissionResult
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import kotlin.time.Duration

class AssetConversionExchangeFactory(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val remoteStorageSource: StorageDataSource,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi,
    private val chainStateRepository: ChainStateRepository,
    private val deductionUseCase: AssetInAdditionalSwapDeductionUseCase,
    private val xcmVersionDetector: XcmVersionDetector,
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
            xcmVersionDetector = xcmVersionDetector
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
) : AssetExchange {

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

                add(AssetConversionEdge(firstAsset, secondAsset))
                add(AssetConversionEdge(secondAsset, firstAsset))
            }
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

    private inner class AssetConversionEdge(fromAsset: Chain.Asset, toAsset: Chain.Asset) : BaseSwapGraphEdge(fromAsset, toAsset) {

        override suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation {
            return AssetConversionOperation(args, fromAsset, toAsset)
        }

        override suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation? {
            return null
        }

        override suspend fun beginOperationPrototype(): AtomicSwapOperationPrototype {
            return AssetConversionOperationPrototype(fromAsset.chainId)
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

        override val weight: Int
            get() = Weights.AssetConversion.SWAP
    }

    inner class AssetConversionOperationPrototype(override val fromChain: ChainId) : AtomicSwapOperationPrototype {

        override suspend fun roughlyEstimateNativeFee(usdConverter: UsdConverter): BigDecimal {
            // in DOT
            return 0.0015.toBigDecimal()
        }

        override suspend fun maximumExecutionTime(): Duration {
            return chainStateRepository.expectedBlockTime(chain.id)
        }
    }

    inner class AssetConversionOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val fromAsset: Chain.Asset,
        private val toAsset: Chain.Asset
    ) : AtomicSwapOperation {

        override val estimatedSwapLimit: SwapLimit = transactionArgs.estimatedSwapLimit

        override val assetOut: FullChainAssetId = toAsset.fullId

        override val assetIn: FullChainAssetId = fromAsset.fullId

        override suspend fun constructDisplayData(): AtomicOperationDisplayData {
            return AtomicOperationDisplayData.Swap(
                from = fromAsset.fullId.withAmount(estimatedSwapLimit.estimatedAmountIn),
                to = toAsset.fullId.withAmount(estimatedSwapLimit.estimatedAmountOut),
            )
        }

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val submissionFee = swapHost.extrinsicService().estimateFee(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    feePaymentCurrency = transactionArgs.feePaymentCurrency
                )
            ) {
                executeSwap(swapLimit = estimatedSwapLimit, sendTo = chain.emptyAccountId())
            }

            return SubmissionOnlyAtomicSwapOperationFee(submissionFee)
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
            return submitInternal(args)
                .mapCatching {
                    SwapExecutionCorrection(
                        actualReceivedAmount = it.requireOutcomeOk().emittedEvents.determineActualSwappedAmount()
                    )
                }
        }

        override suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapSubmissionResult> {
            return submitInternal(args)
                .map { SwapSubmissionResult(it.submissionHierarchy) }
        }

        private suspend fun submitInternal(args: AtomicSwapOperationSubmissionArgs): Result<ExtrinsicExecutionResult> {
            return swapHost.extrinsicService().submitExtrinsicAndAwaitExecution(
                chain = chain,
                origin = TransactionOrigin.SelectedWallet,
                submissionOptions = ExtrinsicService.SubmissionOptions(
                    feePaymentCurrency = transactionArgs.feePaymentCurrency
                )
            ) { buildingContext ->
                // Send swapped funds to the executingAccount since it the account doing the swap
                executeSwap(swapLimit = args.actualSwapLimit, sendTo = buildingContext.submissionOrigin.executingAccount)
            }.requireOk()
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
