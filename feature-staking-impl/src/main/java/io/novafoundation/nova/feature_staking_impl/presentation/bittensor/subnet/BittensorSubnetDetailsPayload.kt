package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.analytics.SubnetDetailsSource
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class BittensorSubnetDetailsPayload(
    val subnetId: Int,
    val replaceAmount: Boolean = false,
    val validatorAddress: String? = null,
    val selectionState: BittensorSelectionState = BittensorSelectionState(),
    val analyticsSource: SubnetDetailsSource = SubnetDetailsSource.PICKER
) : Parcelable
