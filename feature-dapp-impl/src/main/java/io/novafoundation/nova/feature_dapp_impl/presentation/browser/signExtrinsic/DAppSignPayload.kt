package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import android.os.Parcelable
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxRequest
import kotlinx.android.parcel.Parcelize

@Parcelize
class DAppSignPayload(
    val requestId: String,
    val body: ConfirmTxRequest.Payload,
    val dappUrl: String
) : Parcelable
