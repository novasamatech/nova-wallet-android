package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model

import androidx.annotation.ColorRes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel

data class ReferendaGroupModel(val name: String, val badge: String)

data class ReferendumModel(
    val id: ReferendumId,
    val status: ReferendumStatusModel,
    val name: String,
    val timeEstimation: ReferendumTimeEstimation?,
    val track: ReferendumTrackModel?,
    val number: String,
    val voting: ReferendumVotingModel?,
    val yourVote: YourVotePreviewModel?
)

data class YourVotePreviewModel(val voteType: String, @ColorRes val colorRes: Int, val details: String)


