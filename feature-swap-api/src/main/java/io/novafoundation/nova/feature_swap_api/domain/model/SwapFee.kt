package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.data.model.getAmount
import io.novafoundation.nova.feature_account_api.data.model.totalAmount
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class SwapFee(
    val segments: List<SwapSegment>,

    /**
     *  Fees for second and subsequent segments converted to assetIn
     */
    val intermediateSegmentFeesInAssetIn: FeeBase,

    private val additionalMaxAmountDeduction: Balance,
) : Fee, MaxAvailableDeduction {

    data class SwapSegment(val fee: AtomicSwapOperationFee, val operation: AtomicSwapOperation)

    private val firstSegmentFee = segments.first().fee

    private val submissionFee = firstSegmentFee.submissionFee
    private val postSubmissionFees = firstSegmentFee.postSubmissionFees

    private val assetIn = intermediateSegmentFeesInAssetIn.asset

    // Always in asset in
    val additionalAmountForSwap = additionalAmountForSwap()

    val maxAmountDeductionForAssetIn: Balance = maxAmountDeductionFor(assetIn)

    override fun maxAmountDeductionFor(amountAsset: Chain.Asset): Balance {
      return totalFeeAmount(amountAsset) + additionalMaxAmountDeduction
    }

    fun allBasicFees(): List<FeeBase> {
        return segments.flatMap { it.fee.allBasicFees() }
    }

    private fun totalFeeAmount(amountAsset: Chain.Asset): Balance {
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

    // TODO remove this after reworking swap validations
    override val submissionOrigin: SubmissionOrigin = submissionFee.submissionOrigin
    override val amount: BigInteger = totalFeeAmount(submissionFee.asset)
    override val asset: Chain.Asset = submissionFee.asset
}
