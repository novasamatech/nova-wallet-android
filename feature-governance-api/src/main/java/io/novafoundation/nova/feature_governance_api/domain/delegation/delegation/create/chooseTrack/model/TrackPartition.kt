package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model

import io.novafoundation.nova.feature_governance_api.domain.track.Track

data class TrackPartition(
    val available: List<Track>,
    val alreadyVoted: List<Track>,
    val alreadyDelegated: List<Track>
)

fun TrackPartition.hasUnavailableTracks(): Boolean {
    return alreadyVoted.isNotEmpty() || alreadyDelegated.isNotEmpty()
}
