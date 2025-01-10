package io.novafoundation.nova.app.root.navigation.navigators.staking.nominationPools

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondPayload

class NominationPoolsStakingNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHoldersRegistry), NominationPoolsRouter {

    override fun openSetupBondMore() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsBondMoreGraph).navigateInFirstAttachedContext()
    }

    override fun openConfirmBondMore(payload: NominationPoolsConfirmBondMorePayload) {
        navigationBuilder(R.id.action_nominationPoolsSetupBondMoreFragment_to_nominationPoolsConfirmBondMoreFragment)
            .setArgs(NominationPoolsConfirmBondMoreFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmUnbond(payload: NominationPoolsConfirmUnbondPayload) {
        navigationBuilder(R.id.action_nominationPoolsSetupUnbondFragment_to_nominationPoolsConfirmUnbondFragment)
            .setArgs(NominationPoolsConfirmUnbondFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openRedeem() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsRedeemFragment).navigateInFirstAttachedContext()
    }

    override fun openClaimRewards() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsClaimRewardsFragment).navigateInFirstAttachedContext()
    }

    override fun openSetupUnbond() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsUnbondGraph).navigateInFirstAttachedContext()
    }

    override fun returnToStakingMain() {
        navigationBuilder(R.id.back_to_staking_main).navigateInFirstAttachedContext()
    }

    override fun returnToMain() {
        commonNavigator.returnToMain()
    }
}
