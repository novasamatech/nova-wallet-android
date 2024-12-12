package io.novafoundation.nova.app.root.navigation.navigators.staking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
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
    splitScreenNavigationHolder: SplitScreenNavigationHolder,
    rootNavigationHolder: RootNavigationHolder,
    private val stakingDashboardRouter: StakingDashboardRouter,
    private val commonNavigationHolder: Navigator,
) : BaseNavigator(splitScreenNavigationHolder, rootNavigationHolder), StartMultiStakingRouter {

    override fun openStartStakingLanding(payload: StartStakingLandingPayload) {
        navigationBuilder(R.id.action_mainFragment_to_startStackingLanding)
            .setArgs(StartStakingLandingFragment.getBundle(payload))
            .perform()
    }

    override fun openStartParachainStaking() {
        navigationBuilder(R.id.action_startStakingLandingFragment_to_staking_parachain_start_graph)
            .setArgs(StartParachainStakingFragment.getBundle(StartParachainStakingPayload(StartParachainStakingMode.START)))
            .perform()
    }

    override fun openStartMultiStaking(payload: SetupAmountMultiStakingPayload) {
        navigationBuilder(R.id.action_startStakingLandingFragment_to_start_multi_staking_nav_graph)
            .setArgs(SetupAmountMultiStakingFragment.getBundle(payload))
            .perform()
    }

    override fun openSetupStakingType(payload: SetupStakingTypePayload) {
        navigationBuilder(R.id.action_setupAmountMultiStakingFragment_to_setupStakingType)
            .setArgs(SetupStakingTypeFragment.getArguments(payload))
            .perform()
    }

    override fun openConfirm(payload: ConfirmMultiStakingPayload) {
        navigationBuilder(R.id.action_setupAmountMultiStakingFragment_to_confirmMultiStakingFragment)
            .setArgs(ConfirmMultiStakingFragment.getBundle(payload))
            .perform()
    }

    override fun openSelectedValidators() {
        navigationBuilder(R.id.action_confirmMultiStakingFragment_to_confirmNominationsFragment)
            .perform()
    }

    override fun returnToStakingDashboard() {
        stakingDashboardRouter.returnToStakingDashboard()
    }

    override fun goToWalletDetails(metaId: Long) {
        commonNavigationHolder.openWalletDetails(metaId)
    }
}
