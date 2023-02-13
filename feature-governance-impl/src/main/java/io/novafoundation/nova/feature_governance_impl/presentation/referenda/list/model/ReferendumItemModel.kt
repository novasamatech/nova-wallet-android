package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.voters.VoteDirectionModel

data class ReferendaGroupModel(val name: String, val badge: String)

data class ReferendumModel(
    val id: ReferendumId,
    val status: ReferendumStatusModel,
    val name: String,
    val timeEstimation: ReferendumTimeEstimation?,
    val track: ReferendumTrackModel?,
    val number: String,
    val voting: ReferendumVotingModel?,
    val yourVote: YourMultiVotePreviewModel?
)

data class YourMultiVotePreviewModel(val votes: List<YourVotePreviewModel>)

data class YourVotePreviewModel(val voteDirection: VoteDirectionModel, val details: String)
