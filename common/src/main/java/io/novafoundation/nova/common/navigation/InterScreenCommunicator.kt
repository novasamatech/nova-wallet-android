package io.novafoundation.nova.common.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface InterScreenCommunicator<I, O> : InterScreenRequester<I, O>, InterScreenResponder<I, O>

interface InterScreenRequester<I, O> {

    val latestResponse: O?

    val responseFlow: Flow<O>

    fun openRequest(request: I)
}

interface InterScreenResponder<I, O> {

    val lastInput: I?

    val lastState: O?

    fun respond(response: O)
}

fun <I, O> InterScreenResponder<I, O>.requireLastInput(): I {
    return requireNotNull(lastInput) {
        "No input is set"
    }
}

fun InterScreenRequester<Unit, *>.openRequest() = openRequest(Unit)

fun InterScreenResponder<*, Unit>.respond() = respond(Unit)

suspend fun <I, O> InterScreenRequester<I, O>.awaitResponse(request: I): O {
    val responseFlow = responseFlow

    openRequest(request)

    return responseFlow.first()
}
