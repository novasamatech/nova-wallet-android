package io.novafoundation.nova.app.root.navigation.navigators.staking

import android.os.Bundle
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.StakingDashboardFragment
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkTarget

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter

class StakingDashboardNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), StakingDashboardRouter {

    private var stakingTabNavController: NavController? = null
    private var pendingAction: PendingNavigation? = null

    override val scrollToDashboardTopEvent = MutableLiveData<Event<Unit>>()

    fun setStakingTabNavController(navController: NavController) {
        stakingTabNavController = navController

        pendingAction?.let { action ->
            navController.navigate(action.actionId, action.arguments)
            pendingAction = null
        }
    }

    fun clearStakingTabNavController() {
        stakingTabNavController = null
    }

    override fun openMoreStakingOptions() {
        stakingTabNavController?.navigate(R.id.action_stakingDashboardFragment_to_moreStakingOptionsFragment)
    }

    override fun backInStakingTab() {
        stakingTabNavController?.popBackStack()
    }

    override fun returnToStakingDashboard() {
        navigationBuilder()
            .action(R.id.back_to_main)
            .navigateInFirstAttachedContext()

        returnToStakingTabRoot()
        scrollToDashboardTopEvent.value = Unit.event()
    }

    override fun openStakingDashboard(target: StakingDeepLinkTarget?) {
        stakingTabNavController.performNavigationOrDelay(R.id.action_open_staking, StakingDashboardFragment.getBundle(target))
    }

    private fun returnToStakingTabRoot() {
        stakingTabNavController.performNavigationOrDelay(R.id.return_to_staking_dashboard)
    }

    private fun NavController?.performNavigationOrDelay(actionId: Int, arguments: Bundle? = null) {
        val controller = this

        if (controller != null) {
            controller.navigate(actionId, arguments)
        } else {
            pendingAction = PendingNavigation(actionId, arguments)
        }
    }

    private class PendingNavigation(val actionId: Int, val arguments: Bundle?)
}
