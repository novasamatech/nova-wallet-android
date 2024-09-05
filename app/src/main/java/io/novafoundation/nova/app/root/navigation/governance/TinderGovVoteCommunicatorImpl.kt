package io.novafoundation.nova.app.root.navigation.governance

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksResponder
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteRequester
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteResponder
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.SelectGovernanceTracksFragment

class TinderGovVoteCommunicatorImpl(private val router: GovernanceRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<TinderGovVoteRequester.Request, TinderGovVoteResponder.Response>(navigationHolder),
    TinderGovVoteCommunicator {

    override fun openRequest(request: TinderGovVoteRequester.Request) {
        super.openRequest(request)

        router.openSetupTinderGovVote(SetupVotePayload(request.referendumId))
    }
}
