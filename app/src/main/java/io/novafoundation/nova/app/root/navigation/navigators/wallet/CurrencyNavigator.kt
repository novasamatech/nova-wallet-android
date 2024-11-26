package io.novafoundation.nova.app.root.navigation.navigators.wallet

import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.feature_currency_api.presentation.CurrencyRouter

class CurrencyNavigator(val rootRouter: RootRouter, navigationHolder: MainNavigationHolder) : BaseNavigator(navigationHolder), CurrencyRouter {

    override fun returnToWallet() {
        rootRouter.returnToWallet()
    }
}
