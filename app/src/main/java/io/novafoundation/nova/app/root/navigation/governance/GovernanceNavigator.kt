package io.novafoundation.nova.app.root.navigation.governance

import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload

class GovernanceNavigator(
    private val navigationHolder: NavigationHolder
) : BaseNavigator(navigationHolder), GovernanceRouter {

    private val navController: NavController?
        get() = navigationHolder.navController

    override fun openReferendum(payload: ReferendumDetailsPayload) {
        val bundle = ReferendumDetailsFragment.getBundle(payload)
        navController?.navigate(R.id.action_mainFragment_to_referendum_details, bundle)
    }
}
