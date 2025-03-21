package io.novafoundation.nova.feature_swap_impl.data.assetExchange.xcm

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationSubmissionArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.createAggregated
import io.novafoundation.nova.feature_swap_api.domain.model.estimatedAmountIn
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.ext.emptyAccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class XcmOperation(
    private val finalizedOperations: List<XcmAppendableOperation>,
    private val inProgressOperation: XcmAppendableOperation,
    private val swapXcmBuilderFactory: SwapXcmBuilderFactory,
    private val xcmVersion: XcmVersion = XcmVersion.V4,
    private val chainRegistry: ChainRegistry,
) : AtomicSwapOperation {

    private val firstOperation = finalizedOperations.firstOrNull() ?: inProgressOperation
    private val allOperations = finalizedOperations + inProgressOperation

    override val estimatedSwapLimit = SwapLimit.createAggregated(firstOperation.estimatedSwapLimit, inProgressOperation.estimatedSwapLimit)

    override val assetIn: FullChainAssetId = firstOperation.assetIn

    override val assetOut: FullChainAssetId = inProgressOperation.assetOut

    override suspend fun constructDisplayData(): AtomicOperationDisplayData {
        // TODO
        return firstOperation.constructDisplayData()
    }

    override suspend fun estimateFee(): AtomicSwapOperationFee {
        val chain = chainRegistry.getChain(assetOut.chainId)
        val testBeneficiary = chain.emptyAccountIdKey()

        val message = buildXcmMessage(testBeneficiary)

        return TODO()
    }

    private suspend fun buildXcmMessage(beneficiaryAccountId: AccountIdKey): VersionedXcmMessage {
        val builder = swapXcmBuilderFactory.create(assetIn.chainId, xcmVersion)

        allOperations.forEachIndexed { index, operation ->
            if (index == 0) {
                val asset = operation.assetIn.withAmount(operation.withdrawAmount())
                builder.withdrawAsset(asset)
            }

            val weightLimit = if (index == 0) {
                // not allowed to use `Unlimited` on first BuyExecution,
                // but value here doesn't really matter, message is weighed anyway and fees subtracted based on that
                WeightLimit.Limited(0, 0)
            } else {
                WeightLimit.Unlimited
            }
            // TODO this is not robust
            val feeAmount = operation.estimatedSwapLimit.estimatedAmountIn / 2.toBigInteger()
            builder.buyExecution(operation.assetIn.withAmount(feeAmount), weightLimit)

            operation.appendTo(builder, operation.estimatedSwapLimit)
        }

        builder.depositAsset(MultiAssetFilter.Wild.AllCounted(1), beneficiaryAccountId)

        return builder.build()
    }

    override suspend fun requiredAmountInToGetAmountOut(extraOutAmount: Balance): Balance {
        return allOperations.foldRight(extraOutAmount) { operation, intermediateAmount ->
            operation.requiredAmountInToGetAmountOut(intermediateAmount)
        }
    }

    override suspend fun additionalMaxAmountDeduction(): SwapMaxAdditionalAmountDeduction {
        // TODO we are in xcm, most probably we need to always consider ED?
        return firstOperation.additionalMaxAmountDeduction()
    }

    override suspend fun appendSegment(edge: SwapGraphEdge, args: AtomicSwapOperationArgs): AtomicSwapOperation? {
        val innerAppended = inProgressOperation.appendSegment(edge, args)
        if (innerAppended != null) return XcmOperation(finalizedOperations, inProgressOperation)

        val newOperation = edge.beginOperation(args)
        if (newOperation is XcmAppendableOperation) {
            return XcmOperation(finalizedOperations + inProgressOperation, newOperation)
        }

        return null
    }

    override suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection> {
        return TODO()
    }
}
