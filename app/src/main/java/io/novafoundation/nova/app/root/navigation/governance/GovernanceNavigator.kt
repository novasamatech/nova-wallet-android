package io.novafoundation.nova.app.root.navigation.governance

import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload

class GovernanceNavigator(
    private val navigationHolder: NavigationHolder
) : BaseNavigator(navigationHolder), GovernanceRouter {

    private val navController: NavController?
        get() = navigationHolder.navController

    override fun openReferendum(payload: ReferendumDetailsPayload) {
        val bundle = ReferendumDetailsFragment.getBundle(payload)
        navController?.navigate(R.id.action_mainFragment_to_referendum_details, bundle)
    }

    override fun openReferendumFullDetails(payload: ReferendumFullDetailsPayload) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_referendumFullDetailsFragment,
        args = ReferendumFullDetailsFragment.getBundle(payload)
    )

    override fun openDAppBrowser(initialUrl: String) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_DAppBrowserFragment,
        args = DAppBrowserFragment.getBundle(initialUrl)
    )
}
