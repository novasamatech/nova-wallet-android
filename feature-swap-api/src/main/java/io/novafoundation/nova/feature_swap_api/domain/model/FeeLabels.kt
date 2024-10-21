package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee

interface WithDebugLabel {
    val debugLabel: String
}

class SubmissionFeeWithLabel(
    val fee: SubmissionFee,
    override val debugLabel: String = "Submission"
): WithDebugLabel, SubmissionFee by fee

fun SubmissionFeeWithLabel(fee: SubmissionFee?, debugLabel: String): SubmissionFeeWithLabel? {
    return fee?.let { SubmissionFeeWithLabel(it, debugLabel) }
}

class FeeWithLabel(
    val fee: FeeBase,
    override val debugLabel: String
): WithDebugLabel, FeeBase by fee
