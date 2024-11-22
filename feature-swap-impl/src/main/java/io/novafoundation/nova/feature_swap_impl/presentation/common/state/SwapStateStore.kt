package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

interface SwapStateStore {

    fun setState(state: SwapState)

    fun resetState()

    fun getState(): SwapState?

    fun stateFlow(): Flow<SwapState>
}

fun SwapStateStore.getStateOrThrow(): SwapState {
    return requireNotNull(getState()) {
        "Quote was not set"
    }
}

class InMemorySwapStateStore() : SwapStateStore {

    private var swapState = MutableStateFlow<SwapState?>(null)

    override fun setState(state: SwapState) {
        this.swapState.value = state
    }

    override fun resetState() {
        swapState.value = null
    }

    override fun getState(): SwapState? {
        return swapState.value
    }

    override fun stateFlow(): Flow<SwapState> {
        return swapState.filterNotNull()
    }
}
