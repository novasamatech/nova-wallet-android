package io.novafoundation.nova.feature_governance_impl.domain.track

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.domain.track.Track

fun mapTrackInfoToTrack(trackInfo: TrackInfo): Track {
    return Track(trackInfo.id, trackInfo.name)
}
