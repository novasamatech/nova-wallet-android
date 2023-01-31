package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo

data class TrackPartition(
    val available: List<TrackInfo>,
    val alreadyVoted: List<TrackInfo>,
    val alreadyDelegated: List<TrackInfo>
)

fun TrackPartition.hasUnavailableTracks(): Boolean {
    return alreadyVoted.isNotEmpty() || alreadyDelegated.isNotEmpty()
}
