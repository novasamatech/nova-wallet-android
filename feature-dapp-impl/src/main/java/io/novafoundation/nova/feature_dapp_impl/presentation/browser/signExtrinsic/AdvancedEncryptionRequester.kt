package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicCommunicator.*
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerResult
import kotlinx.android.parcel.Parcelize

interface DAppSignExtrinsicRequester : InterScreenRequester<SignerPayloadJSON, Response>

interface DAppSignExtrinsicResponder : InterScreenResponder<SignerPayloadJSON, Response>

interface DAppSignExtrinsicCommunicator : DAppSignExtrinsicRequester, DAppSignExtrinsicResponder {

    sealed class Response : Parcelable {

        @Parcelize
        object Rejected : Response()

        class Signed(val signerResult: SignerResult)
    }
}
