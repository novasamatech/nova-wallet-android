package io.novafoundation.nova.app.root.navigation.navigators.governance

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteRequester
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteResponder
import kotlinx.coroutines.flow.Flow

class TinderGovVoteCommunicatorImpl(private val router: GovernanceRouter, navigationHolder: SplitScreenNavigationHolder) :
    NavStackInterScreenCommunicator<TinderGovVoteRequester.Request, TinderGovVoteResponder.Response>(navigationHolder),
    TinderGovVoteCommunicator {

    override val responseFlow: Flow<TinderGovVoteResponder.Response>
        get() = clearedResponseFlow()

    override fun openRequest(request: TinderGovVoteRequester.Request) {
        super.openRequest(request)

        router.openSetupTinderGovVote(SetupVotePayload(request.referendumId))
    }
}
