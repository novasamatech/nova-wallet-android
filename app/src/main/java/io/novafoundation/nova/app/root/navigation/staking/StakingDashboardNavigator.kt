package io.novafoundation.nova.app.root.navigation.staking

import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter

class StakingDashboardNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), StakingDashboardRouter {

    private var stakingTabNavController: NavController? = null
    private var pendingAction: Int? = null

    fun setStakingTabNavController(navController: NavController) {
        stakingTabNavController = navController

        if (pendingAction != null) {
            navController.performNavigation(pendingAction!!)
            pendingAction = null
        }
    }

    fun clearStakingTabNavController() {
        stakingTabNavController = null
    }

    override fun openMoreStakingOptions() {
        stakingTabNavController?.performNavigation(R.id.action_stakingDashboardFragment_to_moreStakingOptionsFragment)
    }

    override fun backInStakingTab() {
        stakingTabNavController?.popBackStack()
    }

    override fun returnToStakingTabRoot() {
        stakingTabNavController.performNavigationOrDelay(R.id.return_to_staking_dashboard)
    }

    private fun NavController?.performNavigationOrDelay(actionId: Int) {
        val controller = this

        if (controller != null) {
            controller.performNavigation(actionId)
        } else {
            pendingAction = actionId
        }
    }
}
