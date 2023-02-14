package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.track.Track

data class TrackPreset(
    val type: Type,
    val trackIds: List<TrackId>
) {

    companion object;

    enum class Type {

        ALL, TREASURY, FELLOWSHIP, GOVERNANCE
    }
}

fun TrackPreset.Companion.all(trackIds: List<TrackId>): TrackPreset {
    return TrackPreset(
        type = TrackPreset.Type.ALL,
        trackIds = trackIds
    )
}

@JvmName("allFromTrackInfo")
fun TrackPreset.Companion.all(trackInfos: List<Track>): TrackPreset {
    return all(trackInfos.map(Track::id))
}
