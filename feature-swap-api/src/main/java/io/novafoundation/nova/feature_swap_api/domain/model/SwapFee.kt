package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.data.model.getAmount
import io.novafoundation.nova.feature_account_api.data.model.replacePlanks
import io.novafoundation.nova.feature_account_api.data.model.totalAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapFee(
    val segments: List<SwapSegment>,
    /**
     *  Fees for second and subsequent segments converted to assetIn
     */
    val intermediateSegmentFeesInAssetIn: FeeBase
) : GenericFee, MaxAvailableDeduction {

    data class SwapSegment(val fee: AtomicSwapOperationFee, val operation: AtomicSwapOperation)

    private val firstSegmentFee = segments.first().fee

    private val submissionFee = firstSegmentFee.submissionFee
    private val postSubmissionFees = firstSegmentFee.postSubmissionFees

    private val assetIn = intermediateSegmentFeesInAssetIn.asset

    val additionalAmountForSwap = additionalAmountForSwap()

    // TODO better multi fee display with `segmentsFees`
    override val networkFee: Fee = determineNetworkFee()

    override fun deductionFor(amountAsset: Chain.Asset): Balance {
        val executingAccount = submissionFee.submissionOrigin.executingAccount

        val submissionFeeAmount = submissionFee.getAmount(amountAsset, executingAccount)
        val additionalFeesAmount = postSubmissionFees.paidByAccount.totalAmount(amountAsset, executingAccount)

        return submissionFeeAmount + additionalFeesAmount + additionalAmountForSwap.getAmount(amountAsset)
    }

    private fun additionalAmountForSwap(): FeeBase {
        val amountTakenFromAssetIn = postSubmissionFees.paidFromAmount.totalAmount(assetIn)
        val totalFutureFeeInAssetIn = amountTakenFromAssetIn + intermediateSegmentFeesInAssetIn.amount

        return SubstrateFeeBase(totalFutureFeeInAssetIn, assetIn)
    }

    // TODO this is for simpler understanding of real fee until multi-chain view is developed
    private fun determineNetworkFee(): Fee {
        val submissionFeeAsset = submissionFee.asset
        return submissionFee.replacePlanks(newPlanks = deductionFor(submissionFeeAsset))
    }
}
