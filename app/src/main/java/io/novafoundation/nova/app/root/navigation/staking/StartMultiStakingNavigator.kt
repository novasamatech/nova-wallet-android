package io.novafoundation.nova.app.root.navigation.staking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.StartStakingLandingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingPayload

class StartMultiStakingNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), StartMultiStakingRouter {

    override fun openStartStakingLanding(payload: StartStakingLandingPayload) =performNavigation(
        actionId = R.id.action_mainFragment_to_startStackingLanding,
        args = StartStakingLandingFragment.getBundle(payload)
    )

    override fun openSetupAmount(payload: SetupAmountMultiStakingPayload) = performNavigation(
        actionId = R.id.action_startStakingLandingFragment_to_setupAmountMultiStakingFragment,
        args = SetupAmountMultiStakingFragment.getBundle(payload)
    )

    override fun openSetupStakingType() {
        performNavigation(R.id.action_setupAmountMultiStakingFragment_to_setupStakingType)
    }
}
