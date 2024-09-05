package io.novafoundation.nova.feature_governance_impl.domain.referendum.list.filtering

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus

interface ReferendaFilteringProvider {

    fun filterAvailableToVoteReferenda(referenda: List<ReferendumPreview>, voting: Map<TrackId, Voting>): List<ReferendumPreview>
}

class RealReferendaFilteringProvider : ReferendaFilteringProvider {

    override fun filterAvailableToVoteReferenda(referenda: List<ReferendumPreview>, voting: Map<TrackId, Voting>): List<ReferendumPreview> {
        val delegationTracks = voting.filterValues { it is Voting.Delegating }
            .keys

        return referenda.filter {
            it.status is ReferendumStatus.Ongoing &&
                it.referendumVote == null &&
                it.track?.track?.id !in delegationTracks
        }
    }
}
