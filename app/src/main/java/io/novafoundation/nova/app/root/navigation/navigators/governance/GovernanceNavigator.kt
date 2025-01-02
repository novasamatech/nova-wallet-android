package io.novafoundation.nova.app.root.navigation.navigators.governance

import android.os.Bundle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
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
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsFragment
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoFragment
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmReferendumVoteFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVoteFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersPayload

class GovernanceNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonNavigator: Navigator,
    private val contextManager: ContextManager
) : BaseNavigator(navigationHoldersRegistry), GovernanceRouter {

    override fun openReferendum(payload: ReferendumDetailsPayload) {
        navigationBuilder()
            .addCase(R.id.referendumDetailsFragment, R.id.action_referendumDetailsFragment_to_referendumDetailsFragment)
            .addCase(R.id.referendaSearchFragment, R.id.action_open_referendum_details_from_referenda_search)
            .setFallbackCase(R.id.action_open_referendum_details)
            .setArgs(ReferendumDetailsFragment.getBundle(payload))
            .perform()
    }

    override fun openReferendumFullDetails(payload: ReferendumFullDetailsPayload) {
        navigationBuilder(R.id.action_referendumDetailsFragment_to_referendumFullDetailsFragment)
            .setArgs(ReferendumFullDetailsFragment.getBundle(payload))
            .perform()
    }

    override fun openReferendumVoters(payload: ReferendumVotersPayload) {
        navigationBuilder(R.id.action_referendumDetailsFragment_to_referendumVotersFragment)
            .setArgs(ReferendumVotersFragment.getBundle(payload))
            .perform()
    }

    override fun openSetupReferendumVote(payload: SetupVotePayload) {
        navigationBuilder(R.id.action_referendumDetailsFragment_to_setupVoteReferendumFragment)
            .setArgs(SetupVoteFragment.getBundle(payload))
            .perform()
    }

    override fun openSetupTinderGovVote(payload: SetupVotePayload) {
        navigationBuilder(R.id.action_tinderGovCards_to_setupTinderGovVoteFragment)
            .setArgs(SetupVoteFragment.getBundle(payload))
            .perform()
    }

    override fun backToReferendumDetails() {
        navigationBuilder(R.id.action_confirmReferendumVote_to_referendumDetailsFragment)
            .perform()
    }

    override fun finishUnlockFlow(shouldCloseLocksScreen: Boolean) {
        if (shouldCloseLocksScreen) {
            navigationBuilder(R.id.action_confirmReferendumVote_to_mainFragment)
                .perform()
        } else {
            back()
        }
    }

    override fun openWalletDetails(id: Long) {
        commonNavigator.openWalletDetails(id)
    }

    override fun openAddDelegation() {
        navigationBuilder()
            .addCase(R.id.mainFragment, R.id.action_mainFragment_to_delegation)
            .addCase(R.id.yourDelegationsFragment, R.id.action_yourDelegations_to_delegationList)
            .perform()
    }

    override fun openYourDelegations() {
        navigationBuilder(R.id.action_mainFragment_to_your_delegation)
            .perform()
    }

    override fun openBecomingDelegateTutorial() {
        contextManager.getActivity()?.showBrowser(BuildConfig.DELEGATION_TUTORIAL_URL)
    }

    override fun backToYourDelegations() {
        navigationBuilder(R.id.action_back_to_your_delegations)
            .perform()
    }

    override fun openRevokeDelegationChooseTracks(payload: RevokeDelegationChooseTracksPayload) {
        navigationBuilder(R.id.action_delegateDetailsFragment_to_revokeDelegationChooseTracksFragment)
            .setArgs(RevokeDelegationChooseTracksFragment.getBundle(payload))
            .perform()
    }

    override fun openRevokeDelegationsConfirm(payload: RevokeDelegationConfirmPayload) {
        navigationBuilder(R.id.action_revokeDelegationChooseTracksFragment_to_revokeDelegationConfirmFragment)
            .setArgs(RevokeDelegationConfirmFragment.getBundle(payload))
            .perform()
    }

    override fun openDelegateSearch() {
        navigationBuilder(R.id.action_delegateListFragment_to_delegateSearchFragment)
            .perform()
    }

    override fun openSelectGovernanceTracks(bundle: Bundle) {
        navigationBuilder(R.id.action_open_select_governance_tracks)
            .setArgs(bundle)
            .perform()
    }

    override fun openTinderGovCards() {
        navigationBuilder(R.id.action_openTinderGovCards)
            .perform()
    }

    override fun openTinderGovBasket() {
        navigationBuilder(R.id.action_tinderGovCards_to_tinderGovBasket)
            .perform()
    }

    override fun openConfirmTinderGovVote() {
        navigationBuilder(R.id.action_setupTinderGovBasket_to_confirmTinderGovVote)
            .perform()
    }

    override fun backToTinderGovCards() {
        navigationBuilder(R.id.action_confirmTinderGovVote_to_tinderGovCards)
            .perform()
    }

    override fun openReferendumInfo(payload: ReferendumInfoPayload) {
        navigationBuilder()
            .addCase(R.id.tinderGovCards, R.id.action_tinderGovCards_to_referendumInfo)
            .addCase(R.id.setupTinderGovBasketFragment, R.id.action_setupTinderGovBasket_to_referendumInfo)
            .setArgs(ReferendumInfoFragment.getBundle(payload))
            .perform()
    }

    override fun openReferendaSearch() {
        navigationBuilder(R.id.action_open_referenda_search)
            .perform()
    }

    override fun openReferendaFilters() {
        navigationBuilder(R.id.action_open_referenda_filters)
            .perform()
    }

    override fun openRemoveVotes(payload: RemoveVotesPayload) {
        navigationBuilder(R.id.action_open_remove_votes)
            .setArgs(RemoveVotesFragment.getBundle(payload))
            .perform()
    }

    override fun openDelegateDelegators(payload: DelegateDelegatorsPayload) {
        navigationBuilder(R.id.action_delegateDetailsFragment_to_delegateDelegatorsFragment)
            .setArgs(DelegateDelegatorsFragment.getBundle(payload))
            .perform()
    }

    override fun openDelegateDetails(payload: DelegateDetailsPayload) {
        navigationBuilder()
            .addCase(R.id.delegateListFragment, R.id.action_delegateListFragment_to_delegateDetailsFragment)
            .addCase(R.id.yourDelegationsFragment, R.id.action_yourDelegations_to_delegationDetails)
            .addCase(R.id.delegateSearchFragment, R.id.action_delegateSearchFragment_to_delegateDetailsFragment)
            .setArgs(DelegateDetailsFragment.getBundle(payload))
            .perform()
    }

    override fun openNewDelegationChooseTracks(payload: NewDelegationChooseTracksPayload) {
        navigationBuilder(R.id.action_delegateDetailsFragment_to_selectDelegationTracks)
            .setArgs(NewDelegationChooseTracksFragment.getBundle(payload))
            .perform()
    }

    override fun openNewDelegationChooseAmount(payload: NewDelegationChooseAmountPayload) {
        navigationBuilder(R.id.action_selectDelegationTracks_to_newDelegationChooseAmountFragment)
            .setArgs(NewDelegationChooseAmountFragment.getBundle(payload))
            .perform()
    }

    override fun openNewDelegationConfirm(payload: NewDelegationConfirmPayload) {
        navigationBuilder(R.id.action_newDelegationChooseAmountFragment_to_newDelegationConfirmFragment)
            .setArgs(NewDelegationConfirmFragment.getBundle(payload))
            .perform()
    }

    override fun openVotedReferenda(payload: VotedReferendaPayload) {
        navigationBuilder(R.id.action_delegateDetailsFragment_to_votedReferendaFragment)
            .setArgs(VotedReferendaFragment.getBundle(payload))
            .perform()
    }

    override fun openDelegateFullDescription(payload: DescriptionPayload) {
        navigationBuilder(R.id.action_delegateDetailsFragment_to_delegateFullDescription)
            .setArgs(DescriptionFragment.getBundle(payload))
            .perform()
    }

    override fun openDAppBrowser(url: String) {
        navigationBuilder(R.id.action_referendumDetailsFragment_to_DAppBrowserGraph)
            .setArgs(DAppBrowserFragment.getBundle(DAppBrowserPayload.Address(url)))
            .perform()
    }

    override fun openReferendumDescription(payload: DescriptionPayload) {
        navigationBuilder(R.id.action_referendumDetailsFragment_to_referendumDescription)
            .setArgs(DescriptionFragment.getBundle(payload))
            .perform()
    }

    override fun openConfirmVoteReferendum(payload: ConfirmVoteReferendumPayload) {
        navigationBuilder(R.id.action_setupVoteReferendumFragment_to_confirmReferendumVote)
            .setArgs(ConfirmReferendumVoteFragment.getBundle(payload))
            .perform()
    }

    override fun openGovernanceLocksOverview() {
        navigationBuilder(R.id.action_mainFragment_to_governanceLocksOverview)
            .perform()
    }

    override fun openConfirmGovernanceUnlock() {
        navigationBuilder(R.id.action_governanceLocksOverview_to_confirmGovernanceUnlock)
            .perform()
    }
}
