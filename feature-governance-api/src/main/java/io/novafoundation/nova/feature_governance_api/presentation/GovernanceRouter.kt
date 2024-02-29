package io.novafoundation.nova.feature_governance_api.presentation

import android.os.Bundle
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.description.DescriptionPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.delegators.DelegateDelegatorsPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.detail.main.DelegateDetailsPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseTrack.NewDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.confirm.NewDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.removeVotes.RemoveVotesPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.vote.setup.SetupVoteReferendumPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.voters.ReferendumVotersPayload

interface GovernanceRouter : ReturnableRouter {

    fun openReferendum(payload: ReferendumDetailsPayload)

    fun openReferendaSearch()

    fun openReferendaFilters()

    fun openDAppBrowser(initialUrl: String)

    fun openReferendumDescription(payload: DescriptionPayload)

    fun openReferendumFullDetails(payload: ReferendumFullDetailsPayload)

    fun openReferendumVoters(payload: ReferendumVotersPayload)

    fun openSetupVoteReferendum(payload: SetupVoteReferendumPayload)

    fun openConfirmGovernanceUnlock()

    fun openConfirmVoteReferendum(payload: ConfirmVoteReferendumPayload)

    fun openGovernanceLocksOverview()

    fun backToReferendumDetails()

    fun finishUnlockFlow(shouldCloseLocksScreen: Boolean)

    fun openWalletDetails(id: Long)

    fun openAddDelegation()

    fun openYourDelegations()

    fun openDelegateDetails(payload: DelegateDetailsPayload)

    fun openVotedReferenda(payload: VotedReferendaPayload)

    fun openDelegateFullDescription(payload: DescriptionPayload)

    fun openBecomingDelegateTutorial()

    fun openRemoveVotes(payload: RemoveVotesPayload)

    fun openDelegateDelegators(payload: DelegateDelegatorsPayload)

    fun openNewDelegationChooseTracks(payload: NewDelegationChooseTracksPayload)

    fun openNewDelegationChooseAmount(payload: NewDelegationChooseAmountPayload)

    fun openNewDelegationConfirm(payload: NewDelegationConfirmPayload)

    fun backToYourDelegations()

    fun openRevokeDelegationChooseTracks(payload: RevokeDelegationChooseTracksPayload)

    fun openRevokeDelegationsConfirm(payload: RevokeDelegationConfirmPayload)

    fun openDelegateSearch()

    fun openSelectGovernanceTracks(bundle: Bundle)
}
