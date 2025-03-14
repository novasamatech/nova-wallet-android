package io.novafoundation.nova.app.root.navigation.navigators.builder

import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry

class OpenGraphNavigationBuilder(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val graphId: Int
) : NavigationBuilder(navigationHoldersRegistry) {

    override fun performInternal(navigationHolder: NavigationHolder) {
        navigationHolder.navController?.navigate(graphId, args, navOptions, extras)
    }
}
