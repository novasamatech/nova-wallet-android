package io.novafoundation.nova.app.root.navigation.navigators.builder

import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry

class ActionNavigationBuilder(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val actionId: Int
) : NavigationBuilder(navigationHoldersRegistry) {

    override fun performInternal(navigationHolder: NavigationHolder) {
        performAction(navigationHolder, actionId)
    }
}
