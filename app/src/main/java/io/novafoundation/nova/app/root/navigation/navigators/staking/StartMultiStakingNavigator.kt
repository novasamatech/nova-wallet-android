package io.novafoundation.nova.app.root.navigation.navigators.staking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
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
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val stakingDashboardRouter: StakingDashboardRouter,
    private val commonNavigationHolder: Navigator,
) : BaseNavigator(navigationHoldersRegistry), StartMultiStakingRouter {

    override fun openStartStakingLanding(payload: StartStakingLandingPayload) {
        navigationBuilder().action(R.id.action_mainFragment_to_startStackingLanding)
            .setArgs(StartStakingLandingFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openStartParachainStaking() {
        navigationBuilder().action(R.id.action_startStakingLandingFragment_to_staking_parachain_start_graph)
            .setArgs(StartParachainStakingFragment.getBundle(StartParachainStakingPayload(StartParachainStakingMode.START)))
            .navigateInFirstAttachedContext()
    }

    override fun openStartMultiStaking(payload: SetupAmountMultiStakingPayload) {
        navigationBuilder().action(R.id.action_startStakingLandingFragment_to_start_multi_staking_nav_graph)
            .setArgs(SetupAmountMultiStakingFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSetupStakingType(payload: SetupStakingTypePayload) {
        navigationBuilder().action(R.id.action_setupAmountMultiStakingFragment_to_setupStakingType)
            .setArgs(SetupStakingTypeFragment.getArguments(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirm(payload: ConfirmMultiStakingPayload) {
        navigationBuilder().action(R.id.action_setupAmountMultiStakingFragment_to_confirmMultiStakingFragment)
            .setArgs(ConfirmMultiStakingFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSelectedValidators() {
        navigationBuilder().action(R.id.action_confirmMultiStakingFragment_to_confirmNominationsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun returnToStakingDashboard() {
        stakingDashboardRouter.returnToStakingDashboard()
    }

    override fun goToWalletDetails(metaId: Long) {
        commonNavigationHolder.openWalletDetails(metaId)
    }
}
