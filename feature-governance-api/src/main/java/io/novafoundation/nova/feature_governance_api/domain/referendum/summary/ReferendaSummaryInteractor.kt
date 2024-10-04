package io.novafoundation.nova.feature_governance_api.domain.referendum.summary

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import kotlinx.coroutines.CoroutineScope

interface ReferendaSummaryInteractor {

    suspend fun getReferendaSummaries(ids: List<ReferendumId>, coroutineScope: CoroutineScope): Map<ReferendumId, String>
}
