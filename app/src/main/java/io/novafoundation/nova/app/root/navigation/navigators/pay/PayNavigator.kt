package io.novafoundation.nova.app.root.navigation.navigators.pay

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter

class PayNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHoldersRegistry), PayRouter {

    override fun openSwitchWallet() {
        commonNavigator.openSwitchWallet()
    }

    override fun openShopSearch() {
        navigationBuilder().action(R.id.action_open_shopSearchFragment)
            .navigateInFirstAttachedContext()
    }
}
