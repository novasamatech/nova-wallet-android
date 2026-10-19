package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import kotlinx.parcelize.Parcelize

@Parcelize
class BittensorSubnetAmountPayload(
    val subnetId: Int? = null,
    val validatorAddress: String? = null,
    val existingPosition: Boolean = false,
    val replaceAmount: Boolean = false,
    val quickPickOrder: BittensorQuickPickOrder? = null
) : Parcelable {

    init {
        require((subnetId != null) != (quickPickOrder != null)) { "Expected a subnet or a quick-pick order" }
        require(!existingPosition || subnetId != null) { "An existing position requires its subnet" }
    }
}
