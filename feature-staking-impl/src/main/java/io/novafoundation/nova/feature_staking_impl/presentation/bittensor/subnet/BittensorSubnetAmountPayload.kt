package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class BittensorSubnetAmountPayload(
    val subnetId: Int,
    val validatorAddress: String
) : Parcelable
