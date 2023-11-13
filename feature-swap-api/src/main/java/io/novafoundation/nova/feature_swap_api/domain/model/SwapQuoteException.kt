package io.novafoundation.nova.feature_swap_api.domain.model

sealed class SwapQuoteException : Exception() {

    object NotEnoughLiquidity : SwapQuoteException()
}
