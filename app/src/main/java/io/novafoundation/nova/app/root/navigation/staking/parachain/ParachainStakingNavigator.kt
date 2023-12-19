package io.novafoundation.nova.app.root.navigation.staking.parachain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
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
    navigationHolder: NavigationHolder,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHolder), ParachainStakingRouter {

    override fun openStartStaking(payload: StartParachainStakingPayload) = performNavigation(
        actionId = R.id.action_open_startParachainStakingGraph,
        args = StartParachainStakingFragment.getBundle(payload)
    )

    override fun openConfirmStartStaking(payload: ConfirmStartParachainStakingPayload) = performNavigation(
        actionId = R.id.action_startParachainStakingFragment_to_confirmStartParachainStakingFragment,
        args = ConfirmStartParachainStakingFragment.getBundle(payload)
    )

    override fun openSearchCollator() = performNavigation(R.id.action_selectCollatorFragment_to_searchCollatorFragment)

    override fun openCollatorDetails(payload: StakeTargetDetailsPayload) = performNavigation(
        actionId = R.id.open_validator_details,
        args = ValidatorDetailsFragment.getBundle(payload)
    )

    override fun openWalletDetails(metaId: Long) {
        commonNavigator.openWalletDetails(metaId)
    }

    override fun returnToStakingMain() = performNavigation(R.id.back_to_staking_main)

    override fun returnToStartStaking() = performNavigation(R.id.action_return_to_start_staking)

    override fun openCurrentCollators() = performNavigation(R.id.action_stakingFragment_to_currentCollatorsFragment)

    override fun openUnbond() = performNavigation(R.id.action_open_parachainUnbondGraph)

    override fun openConfirmUnbond(payload: ParachainStakingUnbondConfirmPayload) = performNavigation(
        actionId = R.id.action_parachainStakingUnbondFragment_to_parachainStakingUnbondConfirmFragment,
        args = ParachainStakingUnbondConfirmFragment.getBundle(payload)
    )

    override fun openRedeem() = performNavigation(R.id.action_stakingFragment_to_parachainStakingRedeemFragment)

    override fun openRebond(payload: ParachainStakingRebondPayload) = performNavigation(
        actionId = R.id.action_stakingFragment_to_parachainStakingRebondFragment,
        args = ParachainStakingRebondFragment.getBundle(payload)
    )

    override fun openSetupYieldBoost() = performNavigation(R.id.action_stakingFragment_to_yieldBoostGraph)

    override fun openConfirmYieldBoost(
        payload: YieldBoostConfirmPayload
    ) = performNavigation(
        actionId = R.id.action_setupYieldBoostFragment_to_yieldBoostConfirmFragment,
        args = YieldBoostConfirmFragment.getBundle(payload)
    )
}
