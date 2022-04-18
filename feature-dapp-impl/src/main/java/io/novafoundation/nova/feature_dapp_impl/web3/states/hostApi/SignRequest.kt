package io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmTxRequest(
    val id: String,
    val payload: Payload
) : Parcelable {

    interface Payload : Parcelable {

        val address: String

        val chainId: String?
    }
}

sealed class ConfirmTxResponse : Parcelable {

    abstract val requestId: String

    @Parcelize
    class Rejected(override val requestId: String) : ConfirmTxResponse()

    @Parcelize
    class Signed(override val requestId: String, val signature: String) : ConfirmTxResponse()

    @Parcelize
    class Sent(override val requestId: String, val txHash: String) : ConfirmTxResponse()

    @Parcelize
    class SigningFailed(override val requestId: String) : ConfirmTxResponse()
}

