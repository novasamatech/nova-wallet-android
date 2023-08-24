package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingPayload

interface StartMultiStakingRouter : ReturnableRouter {

    fun openStartStakingLanding(payload: StartStakingLandingPayload)

    fun openSetupAmount(payload: SetupAmountMultiStakingPayload)

    fun openSetupStakingType()

    fun openConfirm(payload: ConfirmMultiStakingPayload)

    fun openSelectedValidators()
}
