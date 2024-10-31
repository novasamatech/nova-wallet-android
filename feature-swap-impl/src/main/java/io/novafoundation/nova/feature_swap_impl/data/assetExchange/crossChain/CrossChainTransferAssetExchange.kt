package io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain

import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.FeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.UsdConverter
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.implementations.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransfersConfiguration
import io.novafoundation.nova.runtime.ext.Geneses
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

class CrossChainTransferAssetExchangeFactory(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) : AssetExchange.MultiChainFactory {

    override suspend fun create(
        swapHost: AssetExchange.SwapHost,
        coroutineScope: CoroutineScope
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
            return CrossChainTransferOperationPrototype(from.chainId, to.chainId)
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
        override val fromChain: ChainId,
        private val toChain: ChainId,
    ): AtomicSwapOperationPrototype {

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

        private fun isChainWithExpensiveCrossChain(chainId: ChainId): Boolean {
            return (chainId == Chain.Geneses.POLKADOT) or  (chainId == Chain.Geneses.POLKADOT_ASSET_HUB)
        }
    }

    inner class CrossChainTransferOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val edge: Edge<FullChainAssetId>,
    ) : AtomicSwapOperation {

        override val estimatedSwapLimit: SwapLimit = transactionArgs.estimatedSwapLimit

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val transfer = createTransfer(amount = estimatedSwapLimit.crossChainTransferAmount)

            val crossChainFee = with(crossChainTransfersUseCase) {
                swapHost.extrinsicService().estimateFee(transfer, computationalScope)
            }

            return AtomicSwapOperationFee(
                submissionFee = SubmissionFeeWithLabel(crossChainFee.submissionFee),
                postSubmissionFees = AtomicSwapOperationFee.PostSubmissionFees(
                    paidByAccount = listOfNotNull(
                        SubmissionFeeWithLabel(crossChainFee.deliveryFee, debugLabel = "Delivery"),
                    ),
                    paidFromAmount = listOf(
                        FeeWithLabel(crossChainFee.executionFee, debugLabel = "Execution")
                    )
                ),
            )
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
}
