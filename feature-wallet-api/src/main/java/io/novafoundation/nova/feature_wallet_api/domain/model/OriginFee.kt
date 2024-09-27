package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleGenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import java.math.BigInteger

typealias OriginGenericFee = SimpleGenericFee<OriginFee>

typealias OriginDecimalFee = GenericDecimalFee<OriginGenericFee>

data class OriginFee(
    val networkFee: Fee,
    val deliveryPart: Fee?
) : Fee {

    override val amount: BigInteger = networkFee.amount + deliveryPart?.amount.orZero()

    override val submissionOrigin: SubmissionOrigin = networkFee.submissionOrigin

    override val asset = networkFee.asset
}

fun OriginDecimalFee.networkFeePart(): GenericDecimalFee<GenericFee> {
    return GenericDecimalFee.from(genericFee.networkFee.networkFee, genericFee.networkFee.asset)
}

fun OriginDecimalFee.deliveryFeePart(): GenericDecimalFee<GenericFee>? {
    return genericFee.networkFee.deliveryPart?.let {
        GenericDecimalFee.from(genericFee.networkFee.deliveryPart, genericFee.networkFee.asset)
    }
}

fun OriginDecimalFee.intoDecimalFeeList(): List<GenericDecimalFee<GenericFee>> {
    return listOfNotNull(
        networkFeePart(),
        deliveryFeePart()
    )
}
