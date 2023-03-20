package io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum

import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline

class OffChainReferendumDetails(
    val title: String?,
    val description: String?,
    val proposerName: String?,
    val proposerAddress: String?,
    val timeLine: List<ReferendumTimeline.Entry>?
)
