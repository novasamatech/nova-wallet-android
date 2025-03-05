package io.novafoundation.nova.feature_swap_api.domain.model

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

    data class SwapSegment(val fee: AtomicSwapOperationFee, val operation: AtomicSwapOperation)

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
