package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.data.model.getAmount
import io.novafoundation.nova.feature_account_api.data.model.totalAmount
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapFee(
    val segments: List<SwapSegment>,

    /**
     *  Fees for second and subsequent segments converted to assetIn
     */
    val intermediateSegmentFeesInAssetIn: FeeBase,

    /**
     * Additional deductions from max amount of asset in that are not directly caused by fees
     */
    val additionalMaxAmountDeduction: SwapMaxAdditionalAmountDeduction,
) : MaxAvailableDeduction {

    data class SwapSegment(
        val fee: AtomicSwapOperationFee,
        val operation: AtomicSwapOperation,
        val netFlow: SegmentNetFlow,
    ) {

        /**
         * Amounts that actually flow through the segment once the Nova service commission is skimmed at its
         * charging operation. Built by [buildSwapSegments].
         *
         * @param amountIn actual amount entering the segment (the previous segment's [amountOut])
         * @param amountOut net expected amount leaving the segment — what the UI shows
         * @param amountOutMin net worst-case amount leaving the segment (slippage floor) — what ED validations check
         */
        class SegmentNetFlow(
            val amountIn: Balance,
            val amountOut: Balance,
            val amountOutMin: Balance,
        )
    }

    val firstSegmentFee = segments.first().fee

    val initialSubmissionFee = firstSegmentFee.submissionFee

    private val initialPostSubmissionFees = firstSegmentFee.postSubmissionFees

    private val assetIn = intermediateSegmentFeesInAssetIn.asset

    // Always in asset in
    val additionalAmountForSwap = additionalAmountForSwap()

    override fun maxAmountDeductionFor(amountAsset: Chain.Asset): Balance {
        return totalFeeAmount(amountAsset) + additionalMaxAmountDeduction(amountAsset)
    }

    fun allBasicFees(): List<FeeBase> {
        return segments.flatMap { it.fee.allBasicFees() }
    }

    fun totalFeeAmount(amountAsset: Chain.Asset): Balance {
        val executingAccount = initialSubmissionFee.submissionOrigin.executingAccount

        val submissionFeeAmount = initialSubmissionFee.getAmount(amountAsset, executingAccount)
        val additionalFeesAmount = initialPostSubmissionFees.paidByAccount.totalAmount(amountAsset, executingAccount)

        return submissionFeeAmount + additionalFeesAmount + additionalAmountForSwap.getAmount(amountAsset)
    }

    private fun additionalMaxAmountDeduction(amountAsset: Chain.Asset): Balance {
        // TODO deducting `fromCountedTowardsEd` from max amount is over-conservative
        // Ideally we should deduct max((fromCountedTowardsEd - (countedTowardsEd - transferable)) , 0)
        return if (amountAsset.fullId == assetIn.fullId) additionalMaxAmountDeduction.fromCountedTowardsEd else Balance.ZERO
    }

    private fun additionalAmountForSwap(): FeeBase {
        val amountTakenFromAssetIn = initialPostSubmissionFees.paidFromAmount.totalAmount(assetIn)
        val totalFutureFeeInAssetIn = amountTakenFromAssetIn + intermediateSegmentFeesInAssetIn.amount

        return SubstrateFeeBase(totalFutureFeeInAssetIn, assetIn)
    }
}

/**
 * Pairs each operation with its fee and resolves how much actually flows through it.
 *
 * The route is a chain: each segment's output is the next segment's input. The Nova service commission is
 * skimmed at its charging segment, and that cut is carried down the chain automatically. Resolved once here so
 * display and validations read the same "shown == charged == validated" amounts instead of the raw per-segment
 * quote, which is always gross.
 */
fun buildSwapSegments(fees: List<AtomicSwapOperationFee>, operations: List<AtomicSwapOperation>): List<SwapFee.SwapSegment> {
    var incomingAmount: Balance? = null

    return fees.zip(operations).map { (fee, operation) ->
        val limit = operation.estimatedSwapLimit
        val grossIn = limit.estimatedAmountIn
        val grossOut = limit.estimatedAmountOut
        val commission = fee.serviceCommission?.amount.orZero()

        // Map the actual incoming amount through this segment's own rate, then skim its commission (zero unless
        // it is the charging operation). First segment has no predecessor, so it keeps its own quoted input.
        val amountIn = incomingAmount ?: grossIn
        val ratedOut = if (grossIn.signum() > 0) amountIn * grossOut / grossIn else grossOut
        val amountOut = (ratedOut - commission).atLeastZero()

        // Reduce the worst-case floor by the same commission proportion this segment ends up with, without
        // compounding slippage across segments (each floor stays independent, matching existing behavior).
        val amountOutMin = if (grossOut.signum() > 0) limit.amountOutMin * amountOut / grossOut else limit.amountOutMin

        incomingAmount = amountOut

        SwapFee.SwapSegment(
            fee = fee,
            operation = operation,
            netFlow = SwapFee.SwapSegment.SegmentNetFlow(amountIn = amountIn, amountOut = amountOut, amountOutMin = amountOutMin)
        )
    }
}
