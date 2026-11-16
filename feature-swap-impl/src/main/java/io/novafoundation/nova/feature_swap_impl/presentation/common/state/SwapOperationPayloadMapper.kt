package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency.Asset.Companion.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.FeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionEstimate
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapRouteEdgeId
import io.novafoundation.nova.feature_swap_api.domain.model.WithDebugLabel
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapFeeAmountPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapFeeComponentPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapFeePayload
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapFeeSegmentPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapQuotePayload
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapRouteEdgePayload
import io.novafoundation.nova.feature_swap_api.presentation.model.mapFromModel
import io.novafoundation.nova.feature_swap_api.presentation.model.toParcel
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class SwapOperationPayloadMapper @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val swapService: SwapService,
) {

    fun toPayload(state: SwapState, metaAccountId: Long, returnToTab: Boolean): SwapOperationPayload {
        val quote = state.quote
        return SwapOperationPayload(
            metaAccountId = metaAccountId,
            quote = SwapQuotePayload(
                assetIn = quote.assetIn.toAssetPayload(),
                assetOut = quote.assetOut.toAssetPayload(),
                amountIn = quote.planksIn,
                amountOut = quote.planksOut,
                priceImpact = quote.priceImpact.inFraction,
                direction = quote.direction.toParcel(),
                route = quote.quotedPath.path.map {
                    SwapRouteEdgePayload(
                        assetIn = it.edge.from.toAssetPayload(),
                        assetOut = it.edge.to.toAssetPayload(),
                        exchange = it.edge.exchangeId,
                        pool = it.edge.poolId?.identifier,
                        quotedAmount = it.quotedAmount,
                        quote = it.quote
                    )
                },
                roughFeeIn = quote.quotedPath.roughFeeEstimation.inAssetIn,
                roughFeeOut = quote.quotedPath.roughFeeEstimation.inAssetOut,
                executionMillis = quote.executionEstimate.atomicOperationsEstimates.map { it.inWholeMilliseconds },
                executionBufferMillis = quote.executionEstimate.additionalBuffer.inWholeMilliseconds
            ),
            slippage = state.slippage.inFraction,
            fee = state.fee.toPayload(),
            returnToTab = returnToTab
        )
    }

    suspend fun fromPayload(payload: SwapOperationPayload, scope: CoroutineScope): SwapState {
        val snapshot = payload.quote
        val route = swapService.resolveRoute(
            snapshot.route.map { SwapRouteEdgeId(it.assetIn.fullChainAssetId, it.assetOut.fullChainAssetId, it.exchange, it.pool) },
            scope
        )
        val direction = snapshot.direction.mapFromModel()
        val quote = SwapQuote(
            amountIn = chainRegistry.asset(snapshot.assetIn.fullChainAssetId).withAmount(snapshot.amountIn),
            amountOut = chainRegistry.asset(snapshot.assetOut.fullChainAssetId).withAmount(snapshot.amountOut),
            priceImpact = snapshot.priceImpact.fractions,
            quotedPath = QuotedPath(
                direction = direction,
                path = route.zip(snapshot.route) { edge, saved -> QuotedEdge(saved.quotedAmount, saved.quote, edge) },
                roughFeeEstimation = PathRoughFeeEstimation(snapshot.roughFeeOut, snapshot.roughFeeIn)
            ),
            executionEstimate = SwapExecutionEstimate(
                snapshot.executionMillis.map { it.milliseconds },
                snapshot.executionBufferMillis.milliseconds
            ),
            direction = direction
        )
        val feeAsset = chainRegistry.asset(payload.fee.segments.first().submissionFee.asset.fullChainAssetId)
        val operations = swapService.constructOperations(
            quote.toExecuteArgs(payload.slippage.fractions, feeAsset.toFeePaymentCurrency(), payload.metaAccountId)
        )
        require(operations.size == payload.fee.segments.size) { "Reviewed swap route changed" }
        val segments = operations.zip(payload.fee.segments) { operation, saved ->
            require(operation.assetIn == saved.assetIn.fullChainAssetId && operation.assetOut == saved.assetOut.fullChainAssetId)
            SwapFee.SwapSegment(
                operation = operation,
                fee = saved.restoreFee(),
                netFlow = SwapFee.SwapSegment.SegmentNetFlow(saved.amountIn, saved.amountOut, saved.amountOutMin)
            )
        }
        val fee = SwapFee(
            segments = segments,
            intermediateSegmentFeesInAssetIn = payload.fee.intermediateFees.restore(),
            additionalMaxAmountDeduction = SwapMaxAdditionalAmountDeduction(payload.fee.maxAdditionalDeduction)
        )
        return SwapState(quote, fee, payload.slippage.fractions)
    }

    private fun SwapFee.toPayload() = SwapFeePayload(
        segments = segments.map { segment ->
            val fee = segment.fee
            SwapFeeSegmentPayload(
                assetIn = segment.operation.assetIn.toAssetPayload(),
                assetOut = segment.operation.assetOut.toAssetPayload(),
                submissionFee = fee.submissionFee.toPayload(),
                paidByAccount = fee.postSubmissionFees.paidByAccount.map { it.toPayload() },
                paidFromAmount = fee.postSubmissionFees.paidFromAmount.map { it.toPayload() },
                serviceCommission = fee.serviceCommission?.toPayload(),
                displayComponents = fee.constructDisplayData().components.map { component ->
                    SwapFeeComponentPayload(
                        crossChain = component.type == AtomicOperationFeeDisplayData.SwapFeeType.CROSS_CHAIN,
                        fees = component.fees.map { it.toPayload() }
                    )
                },
                amountIn = segment.netFlow.amountIn,
                amountOut = segment.netFlow.amountOut,
                amountOutMin = segment.netFlow.amountOutMin
            )
        },
        intermediateFees = intermediateSegmentFeesInAssetIn.toPayload(),
        maxAdditionalDeduction = additionalMaxAmountDeduction.fromCountedTowardsEd
    )

    private fun FeeBase.toPayload(): SwapFeeAmountPayload {
        val origin = (this as? SubmissionFee)?.submissionOrigin
        return SwapFeeAmountPayload(
            asset = asset.toAssetPayload(),
            amount = amount,
            executingAccount = origin?.executingAccount?.copyOf(),
            signingAccount = origin?.signingAccount?.copyOf(),
            label = (this as? WithDebugLabel)?.debugLabel
        )
    }

    private suspend fun SwapFeeAmountPayload.restore(): FeeBase {
        val chainAsset = chainRegistry.asset(asset.fullChainAssetId)
        val executing = executingAccount
        val signing = signingAccount
        return if (executing != null && signing != null) {
            SubstrateFee(amount, SubmissionOrigin(executing.copyOf(), signing.copyOf()), chainAsset)
        } else {
            SubstrateFeeBase(amount, chainAsset)
        }
    }

    private suspend fun SwapFeeAmountPayload.restoreSubmissionFee(): SubmissionFeeWithLabel {
        return SubmissionFeeWithLabel(restore() as SubmissionFee, label ?: "Submission")
    }

    private suspend fun SwapFeeSegmentPayload.restoreFee(): AtomicSwapOperationFee {
        val submission = submissionFee.restoreSubmissionFee()
        val postSubmission = AtomicSwapOperationFee.PostSubmissionFees(
            paidByAccount = paidByAccount.map { it.restoreSubmissionFee() },
            paidFromAmount = paidFromAmount.map { FeeWithLabel(it.restore(), it.label.orEmpty()) }
        )
        val commission = serviceCommission?.restore()
        val display = AtomicOperationFeeDisplayData(
            displayComponents.map { component ->
                AtomicOperationFeeDisplayData.SwapFeeComponentDisplay(
                    fees = component.fees.map { it.restore() },
                    type = if (component.crossChain) {
                        AtomicOperationFeeDisplayData.SwapFeeType.CROSS_CHAIN
                    } else {
                        AtomicOperationFeeDisplayData.SwapFeeType.NETWORK
                    }
                )
            }
        )
        return RestoredOperationFee(submission, postSubmission, commission, display)
    }
}

private class RestoredOperationFee(
    override val submissionFee: SubmissionFeeWithLabel,
    override val postSubmissionFees: AtomicSwapOperationFee.PostSubmissionFees,
    override val serviceCommission: FeeBase?,
    private val displayData: AtomicOperationFeeDisplayData,
) : AtomicSwapOperationFee {

    override fun constructDisplayData(): AtomicOperationFeeDisplayData = displayData
}
