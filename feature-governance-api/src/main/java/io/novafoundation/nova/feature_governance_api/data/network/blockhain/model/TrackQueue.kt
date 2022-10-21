package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

class TrackQueue(val referenda: List<ReferendumId>, val maxSize: Int) {

    class Position(val index: Int, val maxSize: Int)

    companion object;
}

fun TrackQueue.Companion.empty(maxSize: Int): TrackQueue = TrackQueue(emptyList(), maxSize = maxSize)

fun TrackQueue.positionOf(referendumId: ReferendumId): TrackQueue.Position {
    return TrackQueue.Position(
        index = referenda.indexOf(referendumId) + 1,
        maxSize = maxSize
    )
}

fun TrackQueue?.orEmpty(): TrackQueue {
    return this ?: TrackQueue.empty(maxSize = 0)
}
