package io.novafoundation.nova.app.root.navigation

import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

abstract class FlowInterScreenCommunicator<I: Any, O: Any>: InterScreenCommunicator<I, O> {

    private val _responseFlow = MutableStateFlow<O?>(null)
    private var _request: I? = null

    override val latestResponse: O?
        get() = _responseFlow.value

    override val lastState: O?
        get() = latestResponse

    override val responseFlow = _responseFlow.filterNotNull()

    override val lastInput: I?
        get() = _request

    abstract fun dispatchRequest(request: I)

    override fun openRequest(request: I) {
        _request = request
        _responseFlow.value = null

        dispatchRequest(request)
    }

    override fun respond(response: O) {
        _responseFlow.value = response
    }
}
