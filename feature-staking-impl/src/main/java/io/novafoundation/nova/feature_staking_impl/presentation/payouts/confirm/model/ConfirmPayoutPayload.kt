package io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
class ConfirmPayoutPayload(
    val payouts: List<PendingPayoutParcelable>,
    val totalRewardInPlanks: BigInteger
) : Parcelable
