package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.data.model.getAmount

data class OriginFee(
    val submissionFee: SubmissionFee,
    val deliveryFee: SubmissionFee?,
) {

    val totalInSubmissionAsset: FeeBase = createTotalFeeInSubmissionAsset()

    fun replaceSubmissionFee(submissionFee: SubmissionFee): OriginFee {
        return copy(submissionFee = submissionFee)
    }

    private fun createTotalFeeInSubmissionAsset(): FeeBase {
        val submissionAsset = submissionFee.asset
        val totalAmount = submissionFee.amount + deliveryFee?.getAmount(submissionAsset).orZero()
        return SubstrateFeeBase(totalAmount, submissionAsset)
    }
}

fun OriginFee.intoFeeList(): List<Fee> {
    return listOfNotNull(submissionFee, deliveryFee)
}
