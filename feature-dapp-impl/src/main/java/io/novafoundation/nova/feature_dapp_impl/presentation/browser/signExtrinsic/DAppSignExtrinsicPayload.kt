package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import android.os.Parcelable
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import kotlinx.android.parcel.Parcelize

@Parcelize
class DAppSignExtrinsicPayload(
    val requestId: String,
    val signerPayloadJSON: SignerPayloadJSON,
    val dappUrl: String
): Parcelable
