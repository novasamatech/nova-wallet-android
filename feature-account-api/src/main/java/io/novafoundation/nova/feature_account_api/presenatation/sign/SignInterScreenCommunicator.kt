package io.novafoundation.nova.feature_account_api.presenatation.sign

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Response
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.util.UUID

interface SignInterScreenRequester : InterScreenRequester<Request, Response>

interface SignInterScreenResponder : InterScreenResponder<Request, Response>

interface SignInterScreenCommunicator : SignInterScreenRequester, SignInterScreenResponder {

    @Parcelize
    class Request(val id: String) : Parcelable

    sealed class Response : Parcelable {

        abstract val requestId: String

        @Parcelize
        class Signed(val signature: SignatureWrapperParcel, override val requestId: String) : Response()

        @Parcelize
        class Cancelled(override val requestId: String) : Response()
    }
}

suspend fun SignInterScreenRequester.awaitConfirmation(): Response {
    val request = createNewRequest()
    val responsesForRequest = responseFlow.filter { it.requestId == request.id }

    openRequest(request)

    return responsesForRequest.first()
}

private fun createNewRequest(): Request {
    val id = UUID.randomUUID().toString()

    return Request(id)
}

fun Request.cancelled() = Response.Cancelled(id)
fun Request.signed(signature: SignatureWrapper) = Response.Signed(SignatureWrapperParcel(signature), id)
