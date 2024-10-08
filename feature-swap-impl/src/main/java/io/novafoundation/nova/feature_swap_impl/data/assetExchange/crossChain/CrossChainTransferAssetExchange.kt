package io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain

import io.novafoundation.nova.common.utils.awaitTrue
import io.novafoundation.nova.common.utils.emptySubstrateAccountId
import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.math.BigInteger

class CrossChainTransferAssetExchangeFactory(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val chainRegistry: ChainRegistry
) : AssetExchange.MultiChainFactory {

    override suspend fun create(
        parentQuoter: AssetExchange.ParentQuoter,
        coroutineScope: CoroutineScope
    ): AssetExchange {

        return CrossChainTransferAssetExchange(
            crossChainTransfersUseCase = crossChainTransfersUseCase,
            chainRegistry = chainRegistry
        )
    }
}

class CrossChainTransferAssetExchange(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val chainRegistry: ChainRegistry
) : AssetExchange {

    private val synced = MutableStateFlow(false)

    override suspend fun sync() {
        crossChainTransfersUseCase.syncCrossChainConfig()
        synced.value = true
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        synced.awaitTrue()

        return crossChainTransfersUseCase.allDirections().map(::CrossChainTransferEdge)
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
            return "Cross-chain Transfer"
        }

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            // TODO include delivery & execution fees
            return amount
        }
    }

    inner class CrossChainTransferOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val edge: Edge<FullChainAssetId>
    ) : AtomicSwapOperation {

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val chain = chainRegistry.getChain(edge.from.chainId)

            // TODO
            return SubstrateFee(
                amount = BigInteger.ZERO,
                submissionOrigin = SubmissionOrigin.singleOrigin(emptySubstrateAccountId()),
                asset = chain.utilityAsset
            )
        }

        override suspend fun submit(previousStepCorrection: SwapExecutionCorrection?): Result<SwapExecutionCorrection> {
            return Result.failure(UnsupportedOperationException("TODO"))
        }
    }
}
