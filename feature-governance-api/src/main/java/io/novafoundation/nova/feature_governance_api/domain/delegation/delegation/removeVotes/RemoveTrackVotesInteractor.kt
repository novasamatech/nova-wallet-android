package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.removeVotes

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus

interface RemoveTrackVotesInteractor {

    suspend fun calculateFee(trackIds: Collection<TrackId>): Balance

    suspend fun removeTrackVotes(trackIds: Collection<TrackId>): Result<ExtrinsicStatus.InBlock>
}
