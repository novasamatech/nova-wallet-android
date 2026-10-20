package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class BittensorSubnetDetailsPayload(
    val subnetId: Int,
    val replaceAmount: Boolean = false,
    val validatorAddress: String? = null,
    val selectionState: BittensorSelectionState = BittensorSelectionState()
) : Parcelable
