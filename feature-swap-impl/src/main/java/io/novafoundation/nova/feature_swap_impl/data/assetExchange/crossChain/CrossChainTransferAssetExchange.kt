package io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain

import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.addPlanks
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData.SwapFeeComponentDisplay
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.FeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.UsdConverter
import io.novafoundation.nova.feature_swap_api.domain.model.crossChain
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountIn
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.network
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.implementations.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferFee
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration

class CrossChainTransferAssetExchangeFactory(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) : AssetExchange.MultiChainFactory {

    override suspend fun create(
        swapHost: AssetExchange.SwapHost, coroutineScope: CoroutineScope
    ): AssetExchange {

        return CrossChainTransferAssetExchange(
            crossChainTransfersUseCase = crossChainTransfersUseCase,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            computationalScope = coroutineScope,
            swapHost = swapHost
        )
    }
}

class CrossChainTransferAssetExchange(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val computationalScope: CoroutineScope,
    private val swapHost: AssetExchange.SwapHost,
) : AssetExchange {

    private val crossChainConfig = MutableStateFlow<CrossChainTransfersConfiguration?>(null)

    override suspend fun sync() {
        crossChainTransfersUseCase.syncCrossChainConfig()

        crossChainConfig.emit(crossChainTransfersUseCase.getConfiguration())
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        val config = crossChainConfig.firstNotNull()
        return config.availableInDestinations().map(::CrossChainTransferEdge)
    }

    override fun feePaymentOverrides(): List<FeePaymentProviderOverride> {
        return emptyList()
    }

    override fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return emptyFlow()
    }

    inner class CrossChainTransferEdge(
        val delegate: Edge<FullChainAssetId>
    ) : SwapGraphEdge, Edge<FullChainAssetId> by delegate {

        override val weight: Int
            get() = Weights.CrossChainTransfer.TRANSFER

        override suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation {
            return CrossChainTransferOperation(args, this)
        }

        override suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation? {
            return null
        }

        override suspend fun beginOperationPrototype(): AtomicSwapOperationPrototype {
            return CrossChainTransferOperationPrototype(this)
        }

        override suspend fun appendToOperationPrototype(currentTransaction: AtomicSwapOperationPrototype): AtomicSwapOperationPrototype? {
            return null
        }

        override suspend fun debugLabel(): String {
            return "To ${chainRegistry.getChain(delegate.to.chainId).name}"
        }

        override fun shouldIgnoreFeeRequirementAfter(predecessor: SwapGraphEdge): Boolean {
            return false
        }

        override suspend fun canPayNonNativeFeesInIntermediatePosition(): Boolean {
            val config = crossChainConfig.value ?: return false

            // Delivery fees cannot be paid in non-native assets
            return delegate.from.chainId !in config.deliveryFeeConfigurations
        }

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            return amount
        }
    }

    inner class CrossChainTransferOperationPrototype(
        private val edge: Edge<FullChainAssetId>,
    ) : AtomicSwapOperationPrototype {

        override val fromChain: ChainId = edge.from.chainId

        private val toChain: ChainId = edge.to.chainId

        override suspend fun roughlyEstimateNativeFee(usdConverter: UsdConverter): BigDecimal {
            var totalAmount = BigDecimal.ZERO

            if (isChainWithExpensiveCrossChain(fromChain)) {
                totalAmount += usdConverter.nativeAssetEquivalentOf(0.15)
            }

            if (isChainWithExpensiveCrossChain(toChain)) {
                totalAmount += usdConverter.nativeAssetEquivalentOf(0.1)
            }

            if (!(isChainWithExpensiveCrossChain(fromChain) || isChainWithExpensiveCrossChain(toChain))) {
                totalAmount += usdConverter.nativeAssetEquivalentOf(0.01)
            }

            return totalAmount
        }

        override suspend fun maximumExecutionTime(): Duration {
            val (fromChain, fromAsset) = chainRegistry.chainWithAsset(edge.from)
            val (toChain, toAsset) = chainRegistry.chainWithAsset(edge.to)

            val transferDirection = AssetTransferDirection(fromChain, fromAsset, toChain, toAsset)

            return crossChainTransfersUseCase.maximumExecutionTime(transferDirection, computationalScope)
        }

        private fun isChainWithExpensiveCrossChain(chainId: ChainId): Boolean {
            return (chainId == Chain.Geneses.POLKADOT) or (chainId == Chain.Geneses.POLKADOT_ASSET_HUB)
        }
    }

    inner class CrossChainTransferOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val edge: Edge<FullChainAssetId>,
    ) : AtomicSwapOperation {

        override val estimatedSwapLimit: SwapLimit = transactionArgs.estimatedSwapLimit

        override suspend fun constructDisplayData(): AtomicOperationDisplayData {
            return AtomicOperationDisplayData.Transfer(
                from = edge.from,
                to = edge.to,
                amount = estimatedSwapLimit.estimatedAmountIn
            )
        }

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val transfer = createTransfer(amount = estimatedSwapLimit.crossChainTransferAmount)

            val crossChainFee = with(crossChainTransfersUseCase) {
                swapHost.extrinsicService().estimateFee(transfer, computationalScope)
            }

            return CrossChainAtomicOperationFee(crossChainFee)
        }

        override suspend fun requiredAmountInToGetAmountOut(extraOutAmount: Balance): Balance {
            return extraOutAmount
        }

        override suspend fun additionalMaxAmountDeduction(): Balance {
            val (chain, chainAsset) = chainRegistry.chainWithAsset(edge.from)
            return crossChainTransfersUseCase.requiredRemainingAmountAfterTransfer(chainAsset, chain)
        }

        override suspend fun inProgressLabel(): String {
            val chainTo = chainRegistry.getChain(edge.to.chainId)
            val assetFrom = chainRegistry.asset(edge.from)

            return "Transferring ${assetFrom.symbol} to ${chainTo.name}"
        }

        override suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection> {
            val transfer = createTransfer(amount = args.actualSwapLimit.crossChainTransferAmount)

            val outcome = with(crossChainTransfersUseCase) {
                swapHost.extrinsicService().performTransfer(transfer, computationalScope)
            }

            return outcome.map { receivedAmount ->
                SwapExecutionCorrection(receivedAmount)
            }
        }

        private suspend fun createTransfer(amount: Balance): AssetTransferBase {
            val (originChain, originAsset) = chainRegistry.chainWithAsset(edge.from)
            val (destinationChain, destinationAsset) = chainRegistry.chainWithAsset(edge.to)

            val selectedAccount = accountRepository.getSelectedMetaAccount()

            return AssetTransferBase(
                recipient = selectedAccount.requireAddressIn(destinationChain),
                originChain = originChain,
                originChainAsset = originAsset,
                destinationChain = destinationChain,
                destinationChainAsset = destinationAsset,
                feePaymentCurrency = transactionArgs.feePaymentCurrency,
                amountPlanks = amount
            )
        }

        private val SwapLimit.crossChainTransferAmount: Balance
            get() = when (this) {
                is SwapLimit.SpecifiedIn -> amountIn
                is SwapLimit.SpecifiedOut -> amountOut
            }
    }

    private class CrossChainAtomicOperationFee(
        private val crossChainFee: CrossChainTransferFee
    ) : AtomicSwapOperationFee {

        override val submissionFee = SubmissionFeeWithLabel(crossChainFee.submissionFee)

        override val postSubmissionFees = AtomicSwapOperationFee.PostSubmissionFees(
            paidByAccount = listOfNotNull(
                SubmissionFeeWithLabel(crossChainFee.deliveryFee, debugLabel = "Delivery"),
            ), paidFromAmount = listOf(
                FeeWithLabel(crossChainFee.executionFee, debugLabel = "Execution")
            )
        )

        override fun constructDisplayData(): AtomicOperationFeeDisplayData {
            val deliveryFee = crossChainFee.deliveryFee
            val shouldSeparateDeliveryFromExecution = deliveryFee != null && deliveryFee.asset.fullId != crossChainFee.executionFee.asset.fullId

            val crossChainFeeComponentDisplay = if (shouldSeparateDeliveryFromExecution) {
                SwapFeeComponentDisplay.crossChain(crossChainFee.executionFee, deliveryFee!!)
            } else {
                val totalCrossChain = crossChainFee.executionFee.addPlanks(deliveryFee?.amount.orZero())
                SwapFeeComponentDisplay.crossChain(totalCrossChain)
            }

            val submissionFeeComponent = SwapFeeComponentDisplay.network(crossChainFee.submissionFee)

            val components = listOf(submissionFeeComponent, crossChainFeeComponentDisplay)
            return AtomicOperationFeeDisplayData(components)
        }
    }
}
