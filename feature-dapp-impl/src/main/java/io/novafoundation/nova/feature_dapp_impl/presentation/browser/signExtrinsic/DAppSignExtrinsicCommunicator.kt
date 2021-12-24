package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicCommunicator.Response
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

interface DAppSignExtrinsicRequester : InterScreenRequester<DAppSignExtrinsicPayload, Response>

interface DAppSignExtrinsicResponder : InterScreenResponder<DAppSignExtrinsicPayload, Response>

interface DAppSignExtrinsicCommunicator : DAppSignExtrinsicRequester, DAppSignExtrinsicResponder {

    sealed class Response : Parcelable {

        abstract val requestId: String

        @Parcelize
        class Rejected(override val requestId: String) : Response()

        @Parcelize
        class Signed(override val requestId: String, val signature: String) : Response()

        @Parcelize
        class SigningFailed(override val requestId: String): Response()
    }
}

suspend fun DAppSignExtrinsicRequester.awaitConfirmation(request: DAppSignExtrinsicPayload): Response {
    val responsesForRequest = responseFlow.filter { it.requestId == request.requestId }

    openRequest(request)

    return responsesForRequest.first()
}
