package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class OriginFee(
    val submissionFee: Fee,
    val deliveryFee: Fee?,
    val chainAsset: Chain.Asset
) : Fee {

    override val amount: BigInteger = submissionFee.amount + deliveryFee?.amount.orZero()

    override val submissionOrigin: SubmissionOrigin = submissionFee.submissionOrigin

    override val asset = submissionFee.asset
}

fun OriginFee.intoFeeList(): List<Fee> {
    return listOfNotNull(submissionFee, deliveryFee)
}
