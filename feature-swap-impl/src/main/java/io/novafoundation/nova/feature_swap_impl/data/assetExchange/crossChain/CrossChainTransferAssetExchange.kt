package io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain

import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.common.utils.mapError
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.addPlanks
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
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
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapOperationSubmissionException
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmissionResult
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
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.XcmTransferDryRunOrigin
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.transferFeatures
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.hasDeliveryFee
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.disabledChains
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
        swapHost: AssetExchange.SwapHost
    ): AssetExchange {
        return CrossChainTransferAssetExchange(
            crossChainTransfersUseCase = crossChainTransfersUseCase,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            swapHost = swapHost
        )
    }
}

class CrossChainTransferAssetExchange(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val swapHost: AssetExchange.SwapHost,
) : AssetExchange {

    private val crossChainConfig = MutableStateFlow<CrossChainTransfersConfiguration?>(null)

    override suspend fun sync() {
        crossChainTransfersUseCase.syncCrossChainConfig()

        crossChainConfig.emit(crossChainTransfersUseCase.getConfiguration())
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        val config = crossChainConfig.firstNotNull()
        val disabledChainIds = chainRegistry.disabledChains().mapToSet { it.id }

        return config.availableInDestinations()
            .filter { it.from.chainId !in disabledChainIds && it.to.chainId !in disabledChainIds }
            .map(::CrossChainTransferEdge)
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

        private var canUseXcmExecute: Boolean? = null

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

        override fun predecessorHandlesFees(predecessor: SwapGraphEdge): Boolean {
            return false
        }

        override suspend fun canPayNonNativeFeesInIntermediatePosition(): Boolean {
            // By default, delivery fees are not payable in non native assets
            return !hasDeliveryFees() ||
                // ... but xcm execute allows to workaround it
                canUseXcmExecute()
        }

        override suspend fun canTransferOutWholeAccountBalance(): Boolean {
            // Precisely speaking just checking for delivery fees is not enough
            // AssetTransactor on origin should also use Preserve transfers when executing TransferAssets instruction
            // However it is much harder to check and there are no chains yet that have limitations on AssetTransactor level
            // but don't have delivery fees, so we only check for delivery fees
            return !hasDeliveryFees() ||
                // When direction has delivery fees, xcm execute can be used to pay them from holding, thus allowing to transfer whole balance
                // and also workaround AssetTransactor issue as "Withdraw" instruction doesn't use Preserve transfers but rather use burn
                canUseXcmExecute()
        }

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            return amount
        }

        override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>): Int {
            return Weights.CrossChainTransfer.TRANSFER
        }

        private suspend fun canUseXcmExecute(): Boolean {
            if (canUseXcmExecute == null) {
                canUseXcmExecute = calculateCanUseXcmExecute()
            }

            return canUseXcmExecute!!
        }

        private fun hasDeliveryFees(): Boolean {
            val config = crossChainConfig.value ?: return false
            return config.hasDeliveryFee(delegate.from, delegate.to)
        }

        private suspend fun calculateCanUseXcmExecute(): Boolean {
            val features = crossChainConfig.value?.dynamic?.transferFeatures(delegate.from, delegate.to.chainId) ?: return false
            return crossChainTransfersUseCase.supportsXcmExecute(delegate.from.chainId, features)
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

            return crossChainTransfersUseCase.maximumExecutionTime(transferDirection, swapHost.scope)
        }

        private fun isChainWithExpensiveCrossChain(chainId: ChainId): Boolean {
            return (chainId == Chain.Geneses.POLKADOT) or (chainId == Chain.Geneses.POLKADOT_ASSET_HUB)
        }
    }

    inner class CrossChainTransferOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val edge: Edge<FullChainAssetId>
    ) : AtomicSwapOperation {

        override val estimatedSwapLimit: SwapLimit = transactionArgs.estimatedSwapLimit

        override val assetOut: FullChainAssetId = edge.to

        override val assetIn: FullChainAssetId = edge.from

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
                swapHost.extrinsicService().estimateFee(transfer, swapHost.scope)
            }

            return CrossChainAtomicOperationFee(crossChainFee)
        }

        override suspend fun requiredAmountInToGetAmountOut(extraOutAmount: Balance): Balance {
            return extraOutAmount
        }

        override suspend fun additionalMaxAmountDeduction(): SwapMaxAdditionalAmountDeduction {
            val (originChain, originChainAsset) = chainRegistry.chainWithAsset(edge.from)
            val destinationChain = chainRegistry.getChain(edge.to.chainId)

            return SwapMaxAdditionalAmountDeduction(
                fromCountedTowardsEd = crossChainTransfersUseCase.requiredRemainingAmountAfterTransfer(originChain, originChainAsset, destinationChain)
            )
        }

        override suspend fun execute(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection> {
            val transfer = createTransfer(amount = args.actualSwapLimit.crossChainTransferAmount)

            return dryRunTransfer(transfer)
                .flatMap { with(crossChainTransfersUseCase) { swapHost.extrinsicService().performTransferAndTrackTransfer(transfer, swapHost.scope) } }
                .map(::SwapExecutionCorrection)
        }

        override suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapSubmissionResult> {
            val transfer = createTransfer(amount = args.actualSwapLimit.crossChainTransferAmount)

            return dryRunTransfer(transfer)
                .flatMap { with(crossChainTransfersUseCase) { swapHost.extrinsicService().performTransferOfExactAmount(transfer, swapHost.scope) } }
                .map { SwapSubmissionResult(it.submissionHierarchy) }
        }

        private suspend fun dryRunTransfer(transfer: AssetTransferBase): Result<Unit> {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val origin = metaAccount.requireAccountIdKeyIn(transfer.originChain)

            return crossChainTransfersUseCase.dryRunTransferIfPossible(
                transfer = transfer,
                // We are transferring exact amount, so we use zero for the fee here
                origin = XcmTransferDryRunOrigin.Signed(origin, crossChainFee = Balance.ZERO),
                computationalScope = swapHost.scope
            )
                .mapError { SwapOperationSubmissionException.SimulationFailed() }
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
                SubmissionFeeWithLabel(crossChainFee.postSubmissionByAccount, debugLabel = "Delivery"),
            ),
            paidFromAmount = listOf(
                FeeWithLabel(crossChainFee.postSubmissionFromAmount, debugLabel = "Execution")
            )
        )

        override fun constructDisplayData(): AtomicOperationFeeDisplayData {
            val deliveryFee = crossChainFee.postSubmissionByAccount
            val shouldSeparateDeliveryFromExecution = deliveryFee != null && deliveryFee.asset.fullId != crossChainFee.postSubmissionFromAmount.asset.fullId

            val crossChainFeeComponentDisplay = if (shouldSeparateDeliveryFromExecution) {
                SwapFeeComponentDisplay.crossChain(crossChainFee.postSubmissionFromAmount, deliveryFee!!)
            } else {
                val totalCrossChain = crossChainFee.postSubmissionFromAmount.addPlanks(deliveryFee?.amount.orZero())
                SwapFeeComponentDisplay.crossChain(totalCrossChain)
            }

            val submissionFeeComponent = SwapFeeComponentDisplay.network(crossChainFee.submissionFee)

            val components = listOf(submissionFeeComponent, crossChainFeeComponentDisplay)
            return AtomicOperationFeeDisplayData(components)
        }
    }
}
