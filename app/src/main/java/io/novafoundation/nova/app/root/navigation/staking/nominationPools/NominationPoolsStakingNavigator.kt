package io.novafoundation.nova.app.root.navigation.staking.nominationPools

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload

class NominationPoolsStakingNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), NominationPoolsRouter {

    override fun openSetupBondMore() = performNavigation(R.id.action_stakingFragment_to_PoolsBondMoreGraph)

    override fun openConfirmBondMore(payload: NominationPoolsConfirmBondMorePayload) = performNavigation(
        actionId = R.id.action_nominationPoolsSetupBondMoreFragment_to_nominationPoolsConfirmBondMoreFragment,
        args = NominationPoolsConfirmBondMoreFragment.getBundle(payload)
    )

    override fun openSetupUnbond() = performNavigation(R.id.action_stakingFragment_to_PoolsUnbondGraph)

    override fun returnToStakingMain() = performNavigation(R.id.back_to_staking_main)
}
