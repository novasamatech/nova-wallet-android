package io.novafoundation.nova.app.root.navigation.navigators.buy

import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_buy_impl.presentation.BuyRouter

class BuyNavigator(navigationHoldersRegistry: NavigationHoldersRegistry) : BuyRouter,
    BaseNavigator(navigationHoldersRegistry)
