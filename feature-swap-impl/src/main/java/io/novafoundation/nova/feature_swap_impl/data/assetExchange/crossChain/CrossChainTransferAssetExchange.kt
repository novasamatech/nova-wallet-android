package io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.FeePaymentProviderOverride
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

    override suspend fun sync() {
        crossChainTransfersUseCase.syncCrossChainConfig()
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        return crossChainTransfersUseCase.allDirections().map(::CrossChainTransferEdge)
    }

    override fun feePaymentOverrides(): List<FeePaymentProviderOverride> {
        return emptyList()
    }

    override fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return emptyFlow()
    }

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
        return false
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

        override suspend fun debugLabel(): String {
            return "Transfer"
        }

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            return amount
        }
    }

    inner class CrossChainTransferOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val edge: Edge<FullChainAssetId>
    ) : AtomicSwapOperation {

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val transfer = createTransfer(amount = transactionArgs.swapLimit.crossChainTransferAmount)

            val crossChainFee = with(crossChainTransfersUseCase) {
                swapHost.extrinsicService().estimateFee(transfer, computationalScope)
            }

            return AtomicSwapOperationFee(
                submissionFee = crossChainFee.fromOriginInFeeCurrency,
                additionalFees = listOfNotNull(
                    crossChainFee.fromOriginInNativeCurrency,
                    crossChainFee.fromHoldingRegister
                )
            )
        }

        override suspend fun submit(previousStepCorrection: SwapExecutionCorrection?): Result<SwapExecutionCorrection> {
            return Result.failure(UnsupportedOperationException("TODO"))
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
                // We cannot use slippage since we cannot guarantee slippage compliance in transfers
                is SwapLimit.SpecifiedIn -> amountOutQuote
                is SwapLimit.SpecifiedOut -> amountInQuote
            }
    }
}
