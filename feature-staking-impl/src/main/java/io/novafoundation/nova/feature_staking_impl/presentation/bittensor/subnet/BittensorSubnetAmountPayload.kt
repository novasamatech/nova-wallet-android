package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import kotlinx.parcelize.Parcelize

@Parcelize
class BittensorSubnetAmountPayload(
    val subnetId: Int? = null,
    val validatorAddress: String? = null,
    /** True when [validatorAddress] is the automatic preset shown on subnet details rather than the user's choice. */
    val validatorIsPreset: Boolean = false,
    /** The automatic preset subnet details showed, also when the user picked another validator there. */
    val recommendedAddress: String? = null,
    val existingPosition: Boolean = false,
    val replaceAmount: Boolean = false,
    /** Which setup screen [replaceAmount] replaces: the subnet setup or the root setup the picker was opened from. */
    val replaceOrigin: BittensorReplaceOrigin = BittensorReplaceOrigin.SUBNET_AMOUNT,
    val quickPickOrder: BittensorQuickPickOrder? = null,
    val selectionState: BittensorSelectionState = BittensorSelectionState()
) : Parcelable {

    init {
        require((subnetId != null) != (quickPickOrder != null)) { "Expected a subnet or a quick-pick order" }
        require(!existingPosition || subnetId != null) { "An existing position requires its subnet" }
    }
}
