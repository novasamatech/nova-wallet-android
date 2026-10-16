package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import kotlinx.android.parcel.Parcelize

interface BittensorSelectValidatorCommunicator :
    InterScreenCommunicator<BittensorSelectValidatorCommunicator.Request, BittensorSelectValidatorCommunicator.Response> {

    @Parcelize
    data class Request(val subnetId: Int, val selectedAddress: String?) : Parcelable

    @Parcelize
    data class Response(val subnetId: Int, val address: String) : Parcelable
}
