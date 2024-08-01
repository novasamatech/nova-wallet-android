package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model

import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.timeline.TimelineLayout
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotersModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.YourMultiVoteModel

class ReferendumDetailsModel(
    val track: ReferendumTrackModel?,
    val number: String?,
    val title: String?,
    val description: ShortenedTextModel?,
    val voting: ReferendumVotingModel?,
    val statusModel: ReferendumStatusModel?,
    val yourVote: YourMultiVoteModel?,
    val ayeVoters: VotersModel?,
    val nayVoters: VotersModel?,
    val abstainVoters: VotersModel?,
    val timeEstimation: ReferendumTimeEstimation?,
    val timeline: TimelineLayout.Timeline?
)
