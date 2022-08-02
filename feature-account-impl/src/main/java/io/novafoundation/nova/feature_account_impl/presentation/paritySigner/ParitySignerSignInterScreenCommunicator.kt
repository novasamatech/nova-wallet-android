package io.novafoundation.nova.feature_account_impl.presentation.paritySigner

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator.Response
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

interface ParitySignerSignInterScreenRequester : InterScreenRequester<Request, Response>

interface ParitySignerSignInterScreenResponder : InterScreenResponder<Request, Response>

interface ParitySignerSignInterScreenCommunicator : ParitySignerSignInterScreenRequester, ParitySignerSignInterScreenResponder {

    @kotlinx.android.parcel.Parcelize
    class Request(val id: String) : Parcelable

    sealed class Response : Parcelable {

        abstract val requestId: String

        @Parcelize
        class Signed(val signature: ByteArray, override val requestId: String) : Response()

        @Parcelize
        class Cancelled(override val requestId: String) : Response()
    }
}

suspend fun ParitySignerSignInterScreenRequester.awaitConfirmation(request: Request): Response {
    val responsesForRequest = responseFlow.filter { it.requestId == request.id }

    openRequest(request)

    return responsesForRequest.first()
}
