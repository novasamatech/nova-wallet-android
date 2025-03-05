package io.novafoundation.nova.app.root.navigation.navigators.vote

import androidx.fragment.app.Fragment
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.CrowdloanFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListFragment
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

class VoteNavigator(
    private val commonNavigator: Navigator,
) : VoteRouter {
    override fun getDemocracyFragment(): Fragment {
        return ReferendaListFragment()
    }

    override fun getCrowdloansFragment(): Fragment {
        return CrowdloanFragment()
    }

    override fun openSwitchWallet() {
        commonNavigator.openSwitchWallet()
    }
}
