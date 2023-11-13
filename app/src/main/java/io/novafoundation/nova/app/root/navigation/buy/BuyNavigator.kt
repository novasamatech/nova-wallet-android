package io.novafoundation.nova.app.root.navigation.buy

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_buy_impl.presentation.BuyRouter

class BuyNavigator(navigationHolder: NavigationHolder) : BuyRouter, BaseNavigator(navigationHolder)
