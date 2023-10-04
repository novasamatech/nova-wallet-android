package io.novafoundation.nova.app.root.navigation.swap

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_swap_api.presentation.SwapRouter

class SwapNavigator(
    private val navigationHolder: NavigationHolder
) : BaseNavigator(navigationHolder), SwapRouter {

    override fun openSwapConfirmation() {
        navigationHolder.navController?.navigate(R.id.action_swapMainSettingsFragment_to_swapConfirmationFragment)
    }
}
