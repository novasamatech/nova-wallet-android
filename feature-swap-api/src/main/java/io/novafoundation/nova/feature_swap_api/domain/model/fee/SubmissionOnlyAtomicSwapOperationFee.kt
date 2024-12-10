package io.novafoundation.nova.feature_swap_api.domain.model.fee

import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData.SwapFeeComponentDisplay
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee.PostSubmissionFees

class SubmissionOnlyAtomicSwapOperationFee(submissionFee: SubmissionFee) : AtomicSwapOperationFee {

    override val submissionFee: SubmissionFeeWithLabel = SubmissionFeeWithLabel(submissionFee)

    override val postSubmissionFees: PostSubmissionFees = PostSubmissionFees()

    override fun constructDisplayData(): AtomicOperationFeeDisplayData {
        return AtomicOperationFeeDisplayData(
            components = listOf(
                SwapFeeComponentDisplay(
                    type = AtomicOperationFeeDisplayData.SwapFeeType.NETWORK,
                    fees = listOf(submissionFee)
                )
            ),
        )
    }
}
