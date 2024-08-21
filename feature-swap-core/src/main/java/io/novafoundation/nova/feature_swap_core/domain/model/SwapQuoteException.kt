package io.novafoundation.nova.feature_swap_core.domain.model

sealed class SwapQuoteException : Exception() {

    object NotEnoughLiquidity : SwapQuoteException()
}
