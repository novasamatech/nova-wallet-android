package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteDirectionModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.toDetailsPayload

data class ReferendaGroupModel(val name: String, val badge: String)

data class ReferendumModel(
    val id: ReferendumId,
    val status: ReferendumStatusModel,
    val name: String,
    val timeEstimation: ReferendumTimeEstimation?,
    val track: ReferendumTrackModel?,
    val number: String,
    val voting: ReferendumVotingModel?,
    val yourVote: YourMultiVotePreviewModel?,
    val isOngoing: Boolean
)

data class YourMultiVotePreviewModel(val votes: List<YourVotePreviewModel>)

data class YourVotePreviewModel(val voteDirection: VoteDirectionModel, val details: String)

fun ReferendumModel.toReferendumDetailsPrefilledData(): ReferendumDetailsPayload.PrefilledData {
    return ReferendumDetailsPayload.PrefilledData(
        referendumNumber = number,
        title = name,
        voting = voting?.toDetailsPayload(),
        status = ReferendumDetailsPayload.StatusData(
            status.name,
            status.colorRes
        )
    )
}
