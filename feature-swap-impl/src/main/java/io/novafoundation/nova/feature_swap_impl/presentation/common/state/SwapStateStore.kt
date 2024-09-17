package io.novafoundation.nova.feature_swap_impl.presentation.common.state

interface SwapStateStore {

    fun setState(state: SwapState)

    fun getState(): SwapState?
}

fun SwapStateStore.getStateOrThrow(): SwapState {
    return requireNotNull(getState()) {
        "Quote was not set"
    }
}

class InMemorySwapStateStore() : SwapStateStore {

    private var quote: SwapState? = null

    override fun setState(state: SwapState) {
       this.quote = state
    }

    override fun getState(): SwapState? {
        return quote
    }
}


