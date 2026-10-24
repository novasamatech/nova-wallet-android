package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import kotlinx.android.parcel.Parcelize

interface BittensorSelectValidatorCommunicator :
    InterScreenCommunicator<BittensorSelectValidatorCommunicator.Request, BittensorSelectValidatorCommunicator.Response> {

    /** [recommendedAddress] is the caller's automatic preset, so keeping it is not tracked as a manual validator change. */
    @Parcelize
    data class Request(
        val subnetId: Int,
        val selectedAddress: String?,
        val selectionState: BittensorSelectionState = BittensorSelectionState(),
        val recommendedAddress: String? = null
    ) : Parcelable

    @Parcelize
    data class Response(
        val subnetId: Int?,
        val address: String?,
        val selectionState: BittensorSelectionState
    ) : Parcelable
}
