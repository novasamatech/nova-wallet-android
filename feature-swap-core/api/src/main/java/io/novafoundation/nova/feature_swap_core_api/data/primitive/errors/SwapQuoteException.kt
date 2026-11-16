package io.novafoundation.nova.feature_swap_core_api.data.primitive.errors

sealed class SwapQuoteException : Exception() {

    object NoRoute : SwapQuoteException()

    object NotEnoughLiquidity : SwapQuoteException()
}
