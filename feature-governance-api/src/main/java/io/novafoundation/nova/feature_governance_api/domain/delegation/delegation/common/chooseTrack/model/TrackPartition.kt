package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.track.Track

data class TrackPartition(
    val preCheckedTrackIds: Set<TrackId>,
    val available: List<Track>,
    val alreadyVoted: List<Track>,
    val alreadyDelegated: List<Track>
)

fun TrackPartition.hasUnavailableTracks(): Boolean {
    return alreadyVoted.isNotEmpty() || alreadyDelegated.isNotEmpty()
}
