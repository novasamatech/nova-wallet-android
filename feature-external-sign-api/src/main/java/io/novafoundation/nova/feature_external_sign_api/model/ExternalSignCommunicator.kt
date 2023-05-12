package io.novafoundation.nova.feature_external_sign_api.model

import android.os.Parcelable
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator.Response
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.first

interface ExternalSignRequester : InterScreenRequester<ExternalSignPayload, Response>

interface ExternalSignResponder : InterScreenResponder<ExternalSignPayload, Response>

interface ExternalSignCommunicator : ExternalSignRequester, ExternalSignResponder {

    sealed class Response : Parcelable {

        abstract val requestId: String

        @Parcelize
        class Rejected(override val requestId: String) : Response()

        @Parcelize
        class Signed(override val requestId: String, val signature: String) : Response()

        @Parcelize
        class Sent(override val requestId: String, val txHash: String) : Response()

        @Parcelize
        class SigningFailed(override val requestId: String, val shouldPresent: Boolean = true) : Response()
    }
}

suspend fun ExternalSignRequester.awaitConfirmation(request: ExternalSignPayload): Response {
    openRequest(request)

    return responseFlow.first { it.requestId == request.signRequest.id }
}

fun Throwable.failedSigningIfNotCancelled(requestId: String) = if (this is SigningCancelledException) {
    null
} else {
    Response.SigningFailed(requestId)
}
