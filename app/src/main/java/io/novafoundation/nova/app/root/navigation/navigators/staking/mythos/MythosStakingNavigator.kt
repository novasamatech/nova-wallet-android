package io.novafoundation.nova.app.root.navigation.navigators.staking.mythos

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsFragment

class MythosStakingNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
) : BaseNavigator(navigationHoldersRegistry), MythosStakingRouter {

    override fun openCollatorDetails(payload: StakeTargetDetailsPayload) {
        navigationBuilder()
            .action(R.id.open_validator_details)
            .setArgs(ValidatorDetailsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmStartStaking(payload: ConfirmStartMythosStakingPayload) {
        navigationBuilder()
            .action(R.id.action_startMythosStakingFragment_to_confirmStartMythosStakingFragment)
            .setArgs(ConfirmStartMythosStakingFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openClaimRewards() {
        TODO("Not yet implemented")
    }

    override fun returnToStartStaking() {
        navigationBuilder()
            .action(R.id.action_return_to_start_staking)
            .navigateInFirstAttachedContext()
    }

    override fun returnToStakingMain() {
        navigationBuilder()
            .action(R.id.back_to_staking_main)
            .navigateInFirstAttachedContext()
    }
}
