package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleGenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

typealias OriginGenericFee = SimpleGenericFee<OriginFee>

typealias OriginDecimalFee = GenericDecimalFee<OriginGenericFee>

data class OriginFee(
    val networkFee: Fee,
    val deliveryPart: BigInteger
) : Fee {

    override val amount: BigInteger = networkFee.amount + deliveryPart

    override val submissionOrigin: SubmissionOrigin = networkFee.submissionOrigin

    override fun plus(other: BigInteger): Fee {
        return OriginFee(
            networkFee = networkFee + other,
            deliveryPart = deliveryPart
        )
    }

    companion object {
        fun from(fee: Fee, deliveryPart: BigInteger): OriginFee {
            return OriginFee(
                networkFee = fee,
                deliveryPart = deliveryPart
            )
        }
    }
}

fun OriginDecimalFee.networkFeePart(chainAsset: Chain.Asset): GenericDecimalFee<GenericFee> {
    return GenericDecimalFee.from(genericFee.networkFee.networkFee, chainAsset)
}

fun OriginDecimalFee.deliveryFeePart(chainAsset: Chain.Asset, submissionOrigin: SubmissionOrigin): GenericDecimalFee<GenericFee> {
    val fee = SubstrateFee(
        amount = genericFee.networkFee.deliveryPart,
        submissionOrigin = submissionOrigin
    )
    return GenericDecimalFee.from(fee, chainAsset)
}

fun OriginDecimalFee.intoDecimalFeeList(chainAsset: Chain.Asset, deliveryFeeSubmissionOrigin: SubmissionOrigin): List<GenericDecimalFee<GenericFee>> {
    return listOf(
        networkFeePart(chainAsset),
        deliveryFeePart(chainAsset, deliveryFeeSubmissionOrigin)
    )
}
