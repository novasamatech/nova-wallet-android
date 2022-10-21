package io.novafoundation.nova.app.root.navigation.governance

import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumPayload

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

    override fun openReferendumVoters(payload: ReferendumVotersPayload) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_referendumVotersFragment,
        args = ReferendumVotersFragment.getBundle(payload)
    )

    override fun openSetupVoteReferendum(payload: SetupVoteReferendumPayload) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_setupVoteReferendumFragment,
        args = SetupVoteReferendumFragment.getBundle(payload)
    )

    override fun openDAppBrowser(initialUrl: String) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_DAppBrowserFragment,
        args = DAppBrowserFragment.getBundle(initialUrl)
    )

    override fun openReferendumDescription(payload: ReferendumDescriptionPayload) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_referendumDescription,
        args = ReferendumDescriptionFragment.getBundle(payload)
    )

    override fun openReferendumVoteConfirm() = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_confirmReferendumVote
    )

    override fun openReferendumUnlockConfirm() {
        navController?.navigate(R.id.action_mainFragment_to_confirmReferendumUnlock)
    }
}
