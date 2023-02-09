package io.novafoundation.nova.app.root.navigation.governance

import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
import io.novafoundation.nova.feature_governance_impl.BuildConfig
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionFragment
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.DelegateDelegatorsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.DelegateDelegatorsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack.NewDelegationChooseTracksFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack.NewDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.NewDelegationConfirmFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.NewDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmReferendumVoteFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersPayload

class GovernanceNavigator(
    private val navigationHolder: NavigationHolder,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHolder), GovernanceRouter {

    private val navController: NavController?
        get() = navigationHolder.navController

    override fun openReferendum(payload: ReferendumDetailsPayload) {
        val bundle = ReferendumDetailsFragment.getBundle(payload)
        navController?.navigate(R.id.action_open_referendum_details, bundle)
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

    override fun backToReferendumDetails() = performNavigation(R.id.action_confirmReferendumVote_to_referendumDetailsFragment)

    override fun finishUnlockFlow(shouldCloseLocksScreen: Boolean) {
        if (shouldCloseLocksScreen) {
            performNavigation(R.id.action_confirmReferendumVote_to_mainFragment)
        } else {
            back()
        }
    }

    override fun openAccountDetails(id: Long) {
        commonNavigator.openAccountDetails(id)
    }

    override fun openAddDelegation() {
        performNavigation(
            cases = arrayOf(
                R.id.mainFragment to R.id.action_mainFragment_to_delegation,
                R.id.yourDelegationsFragment to R.id.action_yourDelegations_to_delegationList,
            )
        )
    }

    override fun openYourDelegations() {
        performNavigation(R.id.action_mainFragment_to_your_delegation)
    }

    override fun openBecomingDelegateTutorial() {
        navigationHolder.contextManager.getActivity()
            ?.showBrowser(BuildConfig.DELEGATION_TUTORIAL_URL)
    }

    override fun finishDelegateFlow() = performNavigation(R.id.action_finish_new_delegation_flow)

    override fun openRevokeDelegationChooseTracks(payload: RevokeDelegationChooseTracksPayload) = performNavigation(
        actionId = R.id.action_delegateDetailsFragment_to_revokeDelegationChooseTracksFragment,
        args = RevokeDelegationChooseTracksFragment.getBundle(payload)
    )

    override fun openRemoveVotes(payload: RemoveVotesPayload) = performNavigation(
        actionId = R.id.action_open_remove_votes,
        args = RemoveVotesFragment.getBundle(payload)
    )

    override fun openDelegateDelegators(payload: DelegateDelegatorsPayload) {
        val bundle = DelegateDelegatorsFragment.getBundle(payload)
        return performNavigation(R.id.action_delegateDetailsFragment_to_delegateDelegatorsFragment, args = bundle)
    }

    override fun openDelegateDetails(payload: DelegateDetailsPayload) {
        performNavigation(
            cases = arrayOf(
                R.id.delegateListFragment to R.id.action_delegateListFragment_to_delegateDetailsFragment,
                R.id.yourDelegationsFragment to R.id.action_yourDelegations_to_delegationDetails,
            ),
            args = DelegateDetailsFragment.getBundle(payload)
        )
    }

    override fun openNewDelegationChooseTracks(payload: NewDelegationChooseTracksPayload) = performNavigation(
        actionId = R.id.action_delegateDetailsFragment_to_selectDelegationTracks,
        args = NewDelegationChooseTracksFragment.getBundle(payload)
    )

    override fun openNewDelegationChooseAmount(payload: NewDelegationChooseAmountPayload) = performNavigation(
        actionId = R.id.action_selectDelegationTracks_to_newDelegationChooseAmountFragment,
        args = NewDelegationChooseAmountFragment.getBundle(payload)
    )

    override fun openNewDelegationConfirm(payload: NewDelegationConfirmPayload) = performNavigation(
        actionId = R.id.action_newDelegationChooseAmountFragment_to_newDelegationConfirmFragment,
        args = NewDelegationConfirmFragment.getBundle(payload)
    )

    override fun openVotedReferenda(payload: VotedReferendaPayload) = performNavigation(
        actionId = R.id.action_delegateDetailsFragment_to_votedReferendaFragment,
        args = VotedReferendaFragment.getBundle(payload)
    )

    override fun openDelegateFullDescription(payload: DescriptionPayload) = performNavigation(
        actionId = R.id.action_delegateDetailsFragment_to_delegateFullDescription,
        args = DescriptionFragment.getBundle(payload)
    )

    override fun openDAppBrowser(initialUrl: String) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_DAppBrowserGraph,
        args = DAppBrowserFragment.getBundle(initialUrl)
    )

    override fun openReferendumDescription(payload: DescriptionPayload) = performNavigation(
        actionId = R.id.action_referendumDetailsFragment_to_referendumDescription,
        args = DescriptionFragment.getBundle(payload)
    )

    override fun openConfirmVoteReferendum(payload: ConfirmVoteReferendumPayload) = performNavigation(
        actionId = R.id.action_setupVoteReferendumFragment_to_confirmReferendumVote,
        args = ConfirmReferendumVoteFragment.getBundle(payload)
    )

    override fun openGovernanceLocksOverview() = performNavigation(
        actionId = R.id.action_mainFragment_to_governanceLocksOverview
    )

    override fun openConfirmGovernanceUnlock() = performNavigation(
        actionId = R.id.action_governanceLocksOverview_to_confirmGovernanceUnlock
    )
}
