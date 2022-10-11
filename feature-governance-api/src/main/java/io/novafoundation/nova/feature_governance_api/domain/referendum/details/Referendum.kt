package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus

data class ReferendumDetails(
    val id: ReferendumId,
    val offChainMetadata: OffChainMetadata?,
    val onChainMetadata: OnChainMetadata?,
    val track: ReferendumTrack?,
    val voting: ReferendumVoting?,
    val userVote: AccountVote?,
    val timeline: ReferendumTimeline,
) {

    data class OffChainMetadata(val title: String, val description: String)

    data class OnChainMetadata(val preImage: PreImage)
}

data class ReferendumTimeline(val currentStatus: ReferendumStatus, val pastEntries: List<Entry>) {

    data class Entry(val state: State, val at: Long?) {
        companion object // extensions
    }

    enum class State {
        CREATED, APPROVED, REJECTED, EXECUTED, CANCELLED, KILLED, TIMED_OUT
    }
}
