package io.novafoundation.nova.app.root.navigation

import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

abstract class FlowInterScreenCommunicator<I : Any, O : Any> :
    InterScreenCommunicator<I, O>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private var response: O? = null

    override val responseFlow = singleReplaySharedFlow<O>()

    private var _request: I? = null

    override val latestResponse: O?
        get() = response

    override val lastState: O?
        get() = latestResponse

    override val lastInput: I?
        get() = _request

    abstract fun dispatchRequest(request: I)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun openRequest(request: I) {
        _request = request
        response = null
        responseFlow.resetReplayCache()

        dispatchRequest(request)
    }

    override fun respond(response: O) {
        launch {
            this@FlowInterScreenCommunicator.response = response
            responseFlow.emit(response)
        }
    }
}
