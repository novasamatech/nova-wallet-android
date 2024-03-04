package io.novafoundation.nova.common.mixin.impl

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.api.NetworkStateMixin
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine.State

private const val ATTEMPT_THRESHOLD = 1

// TODO connection status
class NetworkStateProvider : NetworkStateMixin {

    override val showConnectingBarLiveData = /* observe().flatMapLatest(SocketService::networkStateFlow)
        .map { state ->
            val attempts = stateAsAttempting(state)

            attempts != null && attempts > ATTEMPT_THRESHOLD
        }
        .distinctUntilChanged()
        .asLiveData()*/ MutableLiveData(
        false
    )

    private fun stateAsAttempting(state: State): Int? {
        return when (state) {
            is State.Connecting -> state.attempt
            is State.WaitingForReconnect -> state.attempt
            else -> null
        }
    }
}
