package io.novafoundation.nova.app.root.navigation.navigators.gift

import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter

class GiftNavigator(navigationHoldersRegistry: NavigationHoldersRegistry) : GiftRouter, BaseNavigator(navigationHoldersRegistry)
