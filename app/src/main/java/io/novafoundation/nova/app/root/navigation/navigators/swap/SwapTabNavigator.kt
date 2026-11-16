package io.novafoundation.nova.app.root.navigation.navigators.swap

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapTabFragment
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapTabPayload
import java.util.UUID

class SwapTabNavigator(registry: NavigationHoldersRegistry) : BaseNavigator(registry) {

    private var tabNavController: NavController? = null

    private var pendingNavigation: PendingNavigation? = null

    fun attach(navController: NavController) {
        tabNavController = navController
        pendingNavigation?.let { navigation ->
            pendingNavigation = null
            navigateToTab(navController, navigation)
        }
    }

    fun detach() {
        tabNavController = null
    }

    fun openSwaps(payload: SwapSettingsPayload? = null) {
        val navigation = PendingNavigation(payload?.let { SwapTabPayload(UUID.randomUUID().toString(), it) })
        navigationBuilder().action(R.id.back_to_main).navigateInFirstAttachedContext()
        val controller = tabNavController
        if (controller == null) {
            pendingNavigation = navigation
        } else {
            navigateToTab(controller, navigation)
        }
    }

    private fun navigateToTab(controller: NavController, navigation: PendingNavigation) {
        if (controller.currentDestination?.id == R.id.swapsTabFragment && navigation.payload == null) return

        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(controller.graph.startDestination, false)
            .build()
        controller.navigate(R.id.swapsTabFragment, SwapTabFragment.getBundle(navigation.payload), options)
    }

    private class PendingNavigation(val payload: SwapTabPayload?)
}
