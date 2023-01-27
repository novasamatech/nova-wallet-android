package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.removeVotes

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

interface RemoveTrackVotesInteractor {

    suspend fun trackInfosOf(trackIds: Collection<TrackId>): List<Track>

    suspend fun calculateFee(trackIds: Collection<TrackId>): Balance

    suspend fun removeTrackVotes(trackIds: Collection<TrackId>): Result<String>
}
