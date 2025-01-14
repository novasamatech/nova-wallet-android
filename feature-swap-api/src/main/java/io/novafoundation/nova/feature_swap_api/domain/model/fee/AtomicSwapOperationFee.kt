package io.novafoundation.nova.feature_swap_api.domain.model.fee

import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.FeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel

interface AtomicSwapOperationFee {

    /**
     * Fee that is paid when submitting transaction
     */
    val submissionFee: SubmissionFeeWithLabel

    val postSubmissionFees: PostSubmissionFees

    fun constructDisplayData(): AtomicOperationFeeDisplayData

    class PostSubmissionFees(
        /**
         * Post-submission fees paid by (some) origin account.
         * This is typed as `SubmissionFee` as those fee might still use different accounts (e.g. delivery fees are always paid from requested account)
         */
        val paidByAccount: List<SubmissionFeeWithLabel> = emptyList(),

        /**
         * Post-submission fees paid from swapping amount directly. Its payment is isolated and does not involve any withdrawals from accounts
         */
        val paidFromAmount: List<FeeWithLabel> = emptyList()
    )
}
