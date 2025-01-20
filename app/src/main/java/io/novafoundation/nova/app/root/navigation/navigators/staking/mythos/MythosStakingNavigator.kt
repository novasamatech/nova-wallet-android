package io.novafoundation.nova.app.root.navigation.navigators.staking.mythos

import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter

class MythosStakingNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
) : BaseNavigator(navigationHoldersRegistry), MythosStakingRouter {
}
