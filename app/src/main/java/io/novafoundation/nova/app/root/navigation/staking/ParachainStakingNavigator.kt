package io.novafoundation.nova.app.root.navigation.staking

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ParachainStakingNavigator(
    navigationHolder: NavigationHolder,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHolder), ParachainStakingRouter {

    override fun openStartStaking() = performNavigation(R.id.action_mainFragment_to_startParachainStakingGraph)

    override fun openAddAccount(chainId: ChainId, metaId: Long) {
        commonNavigator.openAddAccount(AddAccountPayload.ChainAccount(chainId, metaId))
    }
}
