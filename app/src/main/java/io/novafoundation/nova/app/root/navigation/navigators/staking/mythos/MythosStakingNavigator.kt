package io.novafoundation.nova.app.root.navigation.navigators.staking.mythos

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm.ConfirmUnbondMythosFragment
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm.ConfirmUnbondMythosPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsFragment

class MythosStakingNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val stakingDashboardRouter: StakingDashboardRouter,
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
        navigationBuilder()
            .action(R.id.action_open_mythos_claim_rewards)
            .navigateInFirstAttachedContext()
    }

    override fun openBondMore() {
        navigationBuilder()
            .action(R.id.action_open_MythosBondMoreGraph)
            .navigateInFirstAttachedContext()
    }

    override fun openUnbond() {
        navigationBuilder()
            .action(R.id.action_open_stakingMythosUnbondGraph)
            .navigateInFirstAttachedContext()
    }

    override fun openUnbondConfirm(payload: ConfirmUnbondMythosPayload) {
        navigationBuilder()
            .action(R.id.action_setupUnbondMythosFragment_to_confirmUnbondMythosFragment)
            .setArgs(ConfirmUnbondMythosFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openRedeem() {
        navigationBuilder()
            .action(R.id.action_stakingFragment_to_mythosRedeemFragment)
            .navigateInFirstAttachedContext()
    }

    override fun finishRedeemFlow(redeemConsequences: RedeemConsequences) {
        if (redeemConsequences.willKillStash) {
            stakingDashboardRouter.returnToStakingDashboard()
        } else {
            returnToStakingMain()
        }
    }

    override fun openStakedCollators() {
        navigationBuilder()
            .action(R.id.action_stakingFragment_to_mythosCurrentCollatorsFragment)
            .navigateInFirstAttachedContext()
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
