package io.novafoundation.nova.app.root.navigation.wallet

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.feature_currency_api.presentation.CurrencyRouter

class CurrencyNavigator(val rootRouter: RootRouter, navigationHolder: NavigationHolder) : BaseNavigator(navigationHolder), CurrencyRouter {

    override fun returnToWallet() {
        rootRouter.returnToWallet()
    }
}
