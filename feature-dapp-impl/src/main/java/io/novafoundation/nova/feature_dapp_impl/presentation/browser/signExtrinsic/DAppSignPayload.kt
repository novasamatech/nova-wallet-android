package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import android.os.Parcelable
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class DAppSignPayload(
    val requestId: String,
    val signerPayload: SignerPayload,
    val dappUrl: String
) : Parcelable
