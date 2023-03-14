package io.novafoundation.nova.feature_governance_api.domain.track

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId

class Track(val id: TrackId, val name: String)

fun <V> Map<TrackId, V>.matchWith(tracks: Map<TrackId, Track>): Map<Track, V> {
    return mapKeys { (trackId, _) -> tracks.getValue(trackId) }
}
