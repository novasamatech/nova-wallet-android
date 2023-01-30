package io.novafoundation.nova.feature_governance_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.DelegateDelegatorsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.VotedReferendaPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.RemoveVotesPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.ReferendumVotersPayload

interface GovernanceRouter : ReturnableRouter {

    fun openReferendum(payload: ReferendumDetailsPayload)

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

    fun openAccountDetails(id: Long)

    fun openAddDelegation()

    fun openDelegateDetails(payload: DelegateDetailsPayload)

    fun openVotedReferenda(payload: VotedReferendaPayload)

    fun openDelegateFullDescription(payload: DescriptionPayload)

    fun openBecomingDelegateTutorial()

    fun openRemoveVotes(payload: RemoveVotesPayload)

    fun openDelegateDelegators(payload: DelegateDelegatorsPayload)
}
