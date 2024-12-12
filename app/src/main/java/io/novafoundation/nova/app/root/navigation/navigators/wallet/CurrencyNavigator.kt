package io.novafoundation.nova.app.root.navigation.navigators.wallet

import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.feature_currency_api.presentation.CurrencyRouter

class CurrencyNavigator(
    val rootRouter: RootRouter,
    splitScreenNavigationHolder: SplitScreenNavigationHolder,
    rootNavigationHolder: RootNavigationHolder
) : BaseNavigator(splitScreenNavigationHolder, rootNavigationHolder), CurrencyRouter {

    override fun returnToWallet() {
        rootRouter.returnToWallet()
    }
}
