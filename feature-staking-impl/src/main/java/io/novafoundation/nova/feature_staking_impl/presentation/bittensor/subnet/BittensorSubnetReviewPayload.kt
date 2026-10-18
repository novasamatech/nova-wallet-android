package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
class BittensorSubnetReviewPayload(
    val subnetId: Int,
    val amountInPlanks: BigInteger,
    val metaId: Long,
    val validatorAddress: String
) : Parcelable
