package io.novafoundation.nova.app.root.navigation.staking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter

class ParachainStakingNavigator(navigationHolder: NavigationHolder): BaseNavigator(navigationHolder), ParachainStakingRouter {

    override fun openStartStaking() = performNavigation(R.id.action_mainFragment_to_startParachainStakingGraph)
}
