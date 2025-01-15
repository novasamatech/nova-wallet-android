package io.novafoundation.nova.app.root.navigation.navigators.staking.parachain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.ParachainStakingRebondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.ConfirmStartParachainStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.ParachainStakingUnbondConfirmFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.YieldBoostConfirmFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsFragment

class ParachainStakingNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHoldersRegistry), ParachainStakingRouter {

    override fun openStartStaking(payload: StartParachainStakingPayload) {
        navigationBuilder().action(R.id.action_open_startParachainStakingGraph)
            .setArgs(StartParachainStakingFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmStartStaking(payload: ConfirmStartParachainStakingPayload) {
        navigationBuilder().action(R.id.action_startParachainStakingFragment_to_confirmStartParachainStakingFragment)
            .setArgs(ConfirmStartParachainStakingFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSearchCollator() {
        navigationBuilder().action(R.id.action_selectCollatorFragment_to_searchCollatorFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openCollatorDetails(payload: StakeTargetDetailsPayload) {
        navigationBuilder().action(R.id.open_validator_details)
            .setArgs(ValidatorDetailsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openWalletDetails(metaId: Long) {
        commonNavigator.openWalletDetails(metaId)
    }

    override fun returnToStakingMain() {
        navigationBuilder().action(R.id.back_to_staking_main).navigateInFirstAttachedContext()
    }

    override fun returnToStartStaking() {
        navigationBuilder().action(R.id.action_return_to_start_staking).navigateInFirstAttachedContext()
    }

    override fun openCurrentCollators() {
        navigationBuilder().action(R.id.action_stakingFragment_to_currentCollatorsFragment).navigateInFirstAttachedContext()
    }

    override fun openUnbond() {
        navigationBuilder().action(R.id.action_open_parachainUnbondGraph).navigateInFirstAttachedContext()
    }

    override fun openConfirmUnbond(payload: ParachainStakingUnbondConfirmPayload) {
        navigationBuilder().action(R.id.action_parachainStakingUnbondFragment_to_parachainStakingUnbondConfirmFragment)
            .setArgs(ParachainStakingUnbondConfirmFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openRedeem() {
        navigationBuilder().action(R.id.action_stakingFragment_to_parachainStakingRedeemFragment).navigateInFirstAttachedContext()
    }

    override fun openRebond(payload: ParachainStakingRebondPayload) {
        navigationBuilder().action(R.id.action_stakingFragment_to_parachainStakingRebondFragment)
            .setArgs(ParachainStakingRebondFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSetupYieldBoost() {
        navigationBuilder().action(R.id.action_stakingFragment_to_yieldBoostGraph).navigateInFirstAttachedContext()
    }

    override fun openConfirmYieldBoost(payload: YieldBoostConfirmPayload) {
        navigationBuilder().action(R.id.action_setupYieldBoostFragment_to_yieldBoostConfirmFragment)
            .setArgs(YieldBoostConfirmFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openAddStakingProxy() {
        navigationBuilder().action(R.id.action_open_addStakingProxyFragment).navigateInFirstAttachedContext()
    }
}
