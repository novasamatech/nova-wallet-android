package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo

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
fun TrackPreset.Companion.all(trackInfos: List<TrackInfo>): TrackPreset {
    return all(trackInfos.map(TrackInfo::id))
}
