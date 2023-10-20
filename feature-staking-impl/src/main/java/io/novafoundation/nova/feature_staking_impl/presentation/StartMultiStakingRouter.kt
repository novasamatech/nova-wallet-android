package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.SetupStakingTypePayload

interface StartMultiStakingRouter : ReturnableRouter {

    fun openStartStakingLanding(payload: StartStakingLandingPayload)

    fun openStartParachainStaking()

    fun openStartMultiStaking(payload: SetupAmountMultiStakingPayload)

    fun openSetupStakingType(payload: SetupStakingTypePayload)

    fun openConfirm(payload: ConfirmMultiStakingPayload)

    fun openSelectedValidators()

    fun returnToStakingDashboard()

    fun goToWalletDetails(metaId: Long)
}
