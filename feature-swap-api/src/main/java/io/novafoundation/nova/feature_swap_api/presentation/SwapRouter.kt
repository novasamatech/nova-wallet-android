package io.novafoundation.nova.feature_swap_api.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface SwapRouter : ReturnableRouter {

    fun openSwapConfirmation()
}
