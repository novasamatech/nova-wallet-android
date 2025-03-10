package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import io.novafoundation.nova.feature_governance_api.data.thresold.gov1.Gov1VotingThreshold
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumProposer
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumThreshold
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

data class ReferendumDetails(
    val id: ReferendumId,
    val offChainMetadata: OffChainMetadata?,
    val onChainMetadata: OnChainMetadata?,
    val proposer: ReferendumProposer?,
    val track: ReferendumTrack?,
    val voting: ReferendumVoting?,
    val threshold: ReferendumThreshold?,
    val userVote: ReferendumVote?,
    val timeline: ReferendumTimeline,
    val fullDetails: FullDetails
) {

    data class FullDetails(
        val deposit: Balance?,
        val voteThreshold: Gov1VotingThreshold?,
        val approvalCurve: VotingCurve?,
        val supportCurve: VotingCurve?,
    )

    data class OffChainMetadata(val title: String?, val description: String?)

    data class OnChainMetadata(val preImage: PreImage?, val preImageHash: ByteArray)
}

data class ReferendumTimeline(val currentStatus: ReferendumStatus, val pastEntries: List<Entry>) {

    data class Entry(val state: State, val at: Long?) {
        companion object // extensions
    }

    enum class State {
        CREATED, APPROVED, REJECTED, EXECUTED, CANCELLED, KILLED, TIMED_OUT
    }
}
