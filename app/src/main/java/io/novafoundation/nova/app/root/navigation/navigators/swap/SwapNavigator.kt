package io.novafoundation.nova.app.root.navigation.navigators.swap

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapConfirmationFragment
import io.novafoundation.nova.feature_swap_impl.presentation.execution.SwapExecutionFragment
import io.novafoundation.nova.feature_swap_impl.presentation.route.SwapRouteFragment
import io.novafoundation.nova.feature_swap_impl.presentation.fee.SwapFeeFragment
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter

class SwapNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonDelegate: Navigator
) : BaseNavigator(navigationHoldersRegistry), SwapRouter {

    override fun openSwapRoute(payload: SwapOperationPayload) {
        navigationBuilder().action(R.id.action_open_swapRouteFragment)
            .setArgs(SwapRouteFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSwapFee(payload: SwapOperationPayload) {
        navigationBuilder().action(R.id.action_open_swapFeeFragment)
            .setArgs(SwapFeeFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSwapExecution(payload: SwapOperationPayload) {
        navigationBuilder().action(R.id.action_swapConfirmationFragment_to_swapExecutionFragment)
            .setArgs(SwapExecutionFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSwapConfirmation(payload: SwapOperationPayload) {
        navigationBuilder().action(R.id.action_open_swap_confirmation)
            .setArgs(SwapConfirmationFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun observeSwapRetries(): kotlinx.coroutines.flow.Flow<SwapSettingsPayload.RepeatOperation> {
        val savedState = requireNotNull(currentBackStackEntry).savedStateHandle
        return savedState.getLiveData<SwapSettingsPayload.RepeatOperation?>(RETRY_KEY)
            .asFlow()
            .filterNotNull()
            .onEach { savedState[RETRY_KEY] = null }
    }

    override fun openRetrySwap(payload: SwapSettingsPayload.RepeatOperation) {
        requireNotNull(previousBackStackEntry).savedStateHandle[RETRY_KEY] = payload
        back()
    }

    private companion object {
        const val RETRY_KEY = "swap.retry"
    }

    override fun finishSwap(returnToTab: Boolean) {
        if (returnToTab) {
            back()
        } else {
            navigationBuilder().action(R.id.action_finish_swap_flow)
                .navigateInFirstAttachedContext()
        }
    }

    override fun openMain() {
        commonDelegate.openMain()
    }
}
