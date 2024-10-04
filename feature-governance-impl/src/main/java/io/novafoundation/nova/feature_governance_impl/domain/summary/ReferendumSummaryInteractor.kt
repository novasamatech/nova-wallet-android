package io.novafoundation.nova.feature_governance_impl.domain.summary

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.summary.ReferendaSummaryInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.CoroutineScope

class RealReferendaSummaryInteractor(
    private val governanceSharedState: GovernanceSharedState,
    private val referendaSummarySharedComputation: ReferendaSummarySharedComputation
) : ReferendaSummaryInteractor {

    override suspend fun getReferendaSummaries(ids: List<ReferendumId>, coroutineScope: CoroutineScope): Map<ReferendumId, String> {
        return runCatching {
            referendaSummarySharedComputation.summaries(
                governanceSharedState.selectedOption(),
                ids,
                coroutineScope
            )
        }.getOrNull()
            .orEmpty()
    }
}
