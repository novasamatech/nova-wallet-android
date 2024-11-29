package io.novafoundation.nova.feature_swap_core_api.data.primitive.errors

sealed class SwapQuoteException : Exception() {

    object NotEnoughLiquidity : SwapQuoteException()
}
