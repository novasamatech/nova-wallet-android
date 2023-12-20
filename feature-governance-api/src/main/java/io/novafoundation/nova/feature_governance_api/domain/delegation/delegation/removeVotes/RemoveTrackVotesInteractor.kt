package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.removeVotes

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus

interface RemoveTrackVotesInteractor {

    suspend fun calculateFee(trackIds: Collection<TrackId>): Fee

    suspend fun removeTrackVotes(trackIds: Collection<TrackId>): Result<ExtrinsicStatus.InBlock>
}
