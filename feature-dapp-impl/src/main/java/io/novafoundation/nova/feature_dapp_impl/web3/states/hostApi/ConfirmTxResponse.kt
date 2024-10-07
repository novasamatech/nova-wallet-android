package io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class ConfirmTxResponse : Parcelable {

    abstract val requestId: String

    @Parcelize
    class Rejected(override val requestId: String) : ConfirmTxResponse()

    @Parcelize
    class Signed(override val requestId: String, val signature: String, val modifiedTransaction: String?) : ConfirmTxResponse()

    @Parcelize
    class Sent(override val requestId: String, val txHash: String) : ConfirmTxResponse()

    @Parcelize
    class SigningFailed(override val requestId: String, val shouldPresent: Boolean) : ConfirmTxResponse()

    @Parcelize
    class ChainIsDisabled(override val requestId: String, val chainName: String) : ConfirmTxResponse()
}
