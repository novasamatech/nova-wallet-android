package io.novafoundation.nova.app.root.navigation.navigators.staking.nominationPools

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondPayload

class NominationPoolsStakingNavigator(
    splitScreenNavigationHolder: SplitScreenNavigationHolder,
    rootNavigationHolder: RootNavigationHolder,
    private val commonNavigator: Navigator,
) : BaseNavigator(splitScreenNavigationHolder, rootNavigationHolder), NominationPoolsRouter {

    override fun openSetupBondMore() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsBondMoreGraph).perform()
    }

    override fun openConfirmBondMore(payload: NominationPoolsConfirmBondMorePayload) {
        navigationBuilder(R.id.action_nominationPoolsSetupBondMoreFragment_to_nominationPoolsConfirmBondMoreFragment)
            .setArgs(NominationPoolsConfirmBondMoreFragment.getBundle(payload))
            .perform()
    }

    override fun openConfirmUnbond(payload: NominationPoolsConfirmUnbondPayload) {
        navigationBuilder(R.id.action_nominationPoolsSetupUnbondFragment_to_nominationPoolsConfirmUnbondFragment)
            .setArgs(NominationPoolsConfirmUnbondFragment.getBundle(payload))
            .perform()
    }

    override fun openRedeem() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsRedeemFragment).perform()
    }

    override fun openClaimRewards() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsClaimRewardsFragment).perform()
    }

    override fun openSetupUnbond() {
        navigationBuilder(R.id.action_stakingFragment_to_PoolsUnbondGraph).perform()
    }

    override fun returnToStakingMain() {
        navigationBuilder(R.id.back_to_staking_main).perform()
    }

    override fun returnToMain() {
        commonNavigator.returnToMain()
    }
}
