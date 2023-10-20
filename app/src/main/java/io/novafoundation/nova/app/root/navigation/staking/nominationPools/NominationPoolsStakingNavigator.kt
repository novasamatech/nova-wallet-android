package io.novafoundation.nova.app.root.navigation.staking.nominationPools

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondPayload

class NominationPoolsStakingNavigator(
    navigationHolder: NavigationHolder,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHolder), NominationPoolsRouter {

    override fun openSetupBondMore() = performNavigation(R.id.action_stakingFragment_to_PoolsBondMoreGraph)

    override fun openConfirmBondMore(payload: NominationPoolsConfirmBondMorePayload) = performNavigation(
        actionId = R.id.action_nominationPoolsSetupBondMoreFragment_to_nominationPoolsConfirmBondMoreFragment,
        args = NominationPoolsConfirmBondMoreFragment.getBundle(payload)
    )

    override fun openConfirmUnbond(payload: NominationPoolsConfirmUnbondPayload) = performNavigation(
        actionId = R.id.action_nominationPoolsSetupUnbondFragment_to_nominationPoolsConfirmUnbondFragment,
        args = NominationPoolsConfirmUnbondFragment.getBundle(payload)
    )

    override fun openRedeem() = performNavigation(R.id.action_stakingFragment_to_PoolsRedeemFragment)

    override fun openClaimRewards() = performNavigation(R.id.action_stakingFragment_to_PoolsClaimRewardsFragment)

    override fun openSetupUnbond() = performNavigation(R.id.action_stakingFragment_to_PoolsUnbondGraph)

    override fun returnToStakingMain() = performNavigation(R.id.back_to_staking_main)

    override fun returnToMain() {
        commonNavigator.returnToMain()
    }
}
