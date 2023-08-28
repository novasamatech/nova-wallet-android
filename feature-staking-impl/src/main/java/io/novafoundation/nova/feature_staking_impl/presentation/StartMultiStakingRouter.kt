package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypePayload

interface StartMultiStakingRouter : ReturnableRouter {

    fun openStartStakingLanding(payload: StartStakingLandingPayload)

    fun openSetupAmount(payload: SetupAmountMultiStakingPayload)

    fun openSetupStakingType(payload: SetupStakingTypePayload)
}
