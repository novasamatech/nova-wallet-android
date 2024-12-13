package io.novafoundation.nova.app.root.navigation.navigators.staking.parachain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
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
        navigationBuilder(R.id.action_open_startParachainStakingGraph)
            .setArgs(StartParachainStakingFragment.getBundle(payload))
            .perform()
    }

    override fun openConfirmStartStaking(payload: ConfirmStartParachainStakingPayload) {
        navigationBuilder(R.id.action_startParachainStakingFragment_to_confirmStartParachainStakingFragment)
            .setArgs(ConfirmStartParachainStakingFragment.getBundle(payload))
            .perform()
    }

    override fun openSearchCollator() {
        navigationBuilder(R.id.action_selectCollatorFragment_to_searchCollatorFragment)
            .perform()
    }

    override fun openCollatorDetails(payload: StakeTargetDetailsPayload) {
        navigationBuilder(R.id.open_validator_details)
            .setArgs(ValidatorDetailsFragment.getBundle(payload))
            .perform()
    }

    override fun openWalletDetails(metaId: Long) {
        commonNavigator.openWalletDetails(metaId)
    }

    override fun returnToStakingMain() {
        navigationBuilder(R.id.back_to_staking_main).perform()
    }

    override fun returnToStartStaking() {
        navigationBuilder(R.id.action_return_to_start_staking).perform()
    }

    override fun openCurrentCollators() {
        navigationBuilder(R.id.action_stakingFragment_to_currentCollatorsFragment).perform()
    }

    override fun openUnbond() {
        navigationBuilder(R.id.action_open_parachainUnbondGraph).perform()
    }

    override fun openConfirmUnbond(payload: ParachainStakingUnbondConfirmPayload) {
        navigationBuilder(R.id.action_parachainStakingUnbondFragment_to_parachainStakingUnbondConfirmFragment)
            .setArgs(ParachainStakingUnbondConfirmFragment.getBundle(payload))
            .perform()
    }

    override fun openRedeem() {
        navigationBuilder(R.id.action_stakingFragment_to_parachainStakingRedeemFragment).perform()
    }

    override fun openRebond(payload: ParachainStakingRebondPayload) {
        navigationBuilder(R.id.action_stakingFragment_to_parachainStakingRebondFragment)
            .setArgs(ParachainStakingRebondFragment.getBundle(payload))
            .perform()
    }

    override fun openSetupYieldBoost() {
        navigationBuilder(R.id.action_stakingFragment_to_yieldBoostGraph).perform()
    }

    override fun openConfirmYieldBoost(payload: YieldBoostConfirmPayload) {
        navigationBuilder(R.id.action_setupYieldBoostFragment_to_yieldBoostConfirmFragment)
            .setArgs(YieldBoostConfirmFragment.getBundle(payload))
            .perform()
    }

    override fun openAddStakingProxy() {
        navigationBuilder(R.id.action_open_addStakingProxyFragment).perform()
    }
}
