package io.novafoundation.nova.feature_staking_impl.presentation.payouts.model

import android.os.Parcelable
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.feature_staking_impl.data.model.Payout
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class PendingPayoutParcelable(
    val validatorInfo: ValidatorInfoParcelable,
    val era: BigInteger,
    val amountInPlanks: BigInteger,
    val timeLeftCalculatedAt: Long,
    val timeLeft: Long,
    val closeToExpire: Boolean,
    val pagesToClaim: List<Int>
) : Parcelable {
    @Parcelize
    class ValidatorInfoParcelable(
        val address: String,
        val identityName: String?,
    ) : Parcelable
}

fun mapPendingPayoutParcelToPayout(
    parcelPayoutParcelable: PendingPayoutParcelable
): Payout {
    return Payout(
        validatorStash = parcelPayoutParcelable.validatorInfo.address.toAccountId().intoKey(),
        era = parcelPayoutParcelable.era,
        amount = parcelPayoutParcelable.amountInPlanks,
        pagesToClaim = parcelPayoutParcelable.pagesToClaim
    )
}
