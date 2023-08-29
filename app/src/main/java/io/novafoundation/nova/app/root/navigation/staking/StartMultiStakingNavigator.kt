package io.novafoundation.nova.app.root.navigation.staking

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
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
) : BaseNavigator(navigationHolder), StartMultiStakingRouter {

    override val stakingFlowFinishedEvent = MutableLiveData<Event<Unit>>()

    override fun openStartStakingLanding(payload: StartStakingLandingPayload) = performNavigation(
        actionId = R.id.action_mainFragment_to_startStackingLanding,
        args = StartStakingLandingFragment.getBundle(payload)
    )

    override fun openSetupAmount(payload: SetupAmountMultiStakingPayload) = performNavigation(
        actionId = R.id.action_startStakingLandingFragment_to_setupAmountMultiStakingFragment,
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
        performNavigation(R.id.back_to_main)
        stakingDashboardRouter.returnToStakingTabRoot()
        stakingFlowFinishedEvent.value = Unit.event()
    }
}
