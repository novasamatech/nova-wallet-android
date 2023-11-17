package io.novafoundation.nova.feature_swap_api.domain.model

enum class SwapDirection {
    SPECIFIED_IN, SPECIFIED_OUT
}

fun SwapDirection.flip(): SwapDirection {
    return when (this) {
        SwapDirection.SPECIFIED_IN -> SwapDirection.SPECIFIED_OUT
        SwapDirection.SPECIFIED_OUT -> SwapDirection.SPECIFIED_IN
    }
}
