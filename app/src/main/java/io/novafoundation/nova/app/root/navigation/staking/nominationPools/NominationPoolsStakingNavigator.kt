package io.novafoundation.nova.app.root.navigation.staking.nominationPools

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter

class NominationPoolsStakingNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), NominationPoolsRouter {

    override fun openSetupBondMore() = performNavigation(R.id.action_stakingFragment_to_bondMoreGraph)
}
