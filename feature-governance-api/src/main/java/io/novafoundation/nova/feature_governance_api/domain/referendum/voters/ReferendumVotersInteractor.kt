package io.novafoundation.nova.feature_governance_api.domain.referendum.voters

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import kotlinx.coroutines.flow.Flow

interface ReferendumVotersInteractor {

    fun votersFlow(
        referendumId: ReferendumId,
        type: VoteType
    ): Flow<List<ReferendumVoter>>
}
