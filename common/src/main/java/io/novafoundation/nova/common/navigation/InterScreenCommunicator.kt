package io.novafoundation.nova.common.navigation

import kotlinx.coroutines.flow.Flow

interface InterScreenCommunicator<I, O> : InterScreenRequester<I, O>, InterScreenResponder<I, O>

interface InterScreenRequester<I, O> {

    val latestResponse: O?

    val responseFlow: Flow<O>

    fun openRequest(request: I)
}

interface InterScreenResponder<I, O> {

    fun respond(respond: O)
}
