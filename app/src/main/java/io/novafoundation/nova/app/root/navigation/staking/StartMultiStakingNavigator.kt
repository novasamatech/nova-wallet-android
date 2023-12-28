package io.novafoundation.nova.app.root.navigation.staking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.StartStakingLandingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypeFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypePayload

class StartMultiStakingNavigator(
    navigationHolder: NavigationHolder,
    private val stakingDashboardRouter: StakingDashboardRouter,
    private val commonNavigationHolder: Navigator,
) : BaseNavigator(navigationHolder), StartMultiStakingRouter {

    override fun openStartStakingLanding(payload: StartStakingLandingPayload) = performNavigation(
        actionId = R.id.action_mainFragment_to_startStackingLanding,
        args = StartStakingLandingFragment.getBundle(payload)
    )

    override fun openStartParachainStaking() = performNavigation(
        actionId = R.id.action_startStakingLandingFragment_to_staking_parachain_start_graph,
        args = StartParachainStakingFragment.getBundle(StartParachainStakingPayload(StartParachainStakingMode.START))
    )

    override fun openStartMultiStaking(payload: SetupAmountMultiStakingPayload) = performNavigation(
        actionId = R.id.action_startStakingLandingFragment_to_start_multi_staking_nav_graph,
        args = SetupAmountMultiStakingFragment.getBundle(payload)
    )

    override fun openSetupStakingType(payload: SetupStakingTypePayload) = performNavigation(
        R.id.action_setupAmountMultiStakingFragment_to_setupStakingType,
        args = SetupStakingTypeFragment.getArguments(payload)
    )

    override fun openConfirm(payload: ConfirmMultiStakingPayload) = performNavigation(
        actionId = R.id.action_setupAmountMultiStakingFragment_to_confirmMultiStakingFragment,
        args = ConfirmMultiStakingFragment.getBundle(payload)
    )

    override fun openSelectedValidators() {
        performNavigation(R.id.action_confirmMultiStakingFragment_to_confirmNominationsFragment)
    }

    override fun returnToStakingDashboard() {
        stakingDashboardRouter.returnToStakingDashboard()
    }

    override fun goToWalletDetails(metaId: Long) {
        commonNavigationHolder.openWalletDetails(metaId)
    }
}
