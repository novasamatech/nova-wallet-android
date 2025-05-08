package io.novafoundation.nova.app.root.navigation.navigators.pay

import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter

class PayNavigator(
    private val commonNavigator: Navigator,
) : PayRouter {

    override fun openSwitchWallet() {
        commonNavigator.openSwitchWallet()
    }
}
