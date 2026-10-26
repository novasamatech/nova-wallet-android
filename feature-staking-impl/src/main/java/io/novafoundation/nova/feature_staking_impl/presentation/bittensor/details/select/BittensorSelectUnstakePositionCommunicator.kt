package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import kotlinx.android.parcel.Parcelize

interface BittensorSelectUnstakePositionCommunicator :
    InterScreenCommunicator<BittensorSelectUnstakePositionCommunicator.Request, BittensorSelectUnstakePositionCommunicator.Response> {

    @Parcelize
    data class Option(val position: BittensorExitPositionPayload, val address: String, val amount: String, val fiat: String?) : Parcelable

    @Parcelize
    data class Request(val options: List<Option>) : Parcelable

    @Parcelize
    data class Response(val position: BittensorExitPositionPayload) : Parcelable
}
