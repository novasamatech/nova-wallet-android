package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

class TrackQueue(val referenda: List<ReferendumId>) {

    class Position(val index: Int, val maxSize: Int)

    companion object;
}

fun TrackQueue.Companion.empty(): TrackQueue = TrackQueue(emptyList())

fun TrackQueue.positionOf(referendumId: ReferendumId): TrackQueue.Position {
    return TrackQueue.Position(
        index = referenda.indexOf(referendumId) + 1,
        maxSize = referenda.size
    )
}

fun TrackQueue?.orEmpty(): TrackQueue {
    return this ?: TrackQueue.empty()
}
