package io.novafoundation.nova.feature_governance_api.domain.referendum.list

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumThreshold
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting

enum class ReferendumGroup {
    ONGOING, COMPLETED
}

data class ReferendumPreview(
    val id: ReferendumId,
    val status: ReferendumStatus,
    val offChainMetadata: OffChainMetadata?,
    val onChainMetadata: OnChainMetadata?,
    val track: ReferendumTrack?,
    val voting: ReferendumVoting?,
    val threshold: ReferendumThreshold?,
    val referendumVote: ReferendumVote?,
) {

    data class OffChainMetadata(val title: String)

    data class OnChainMetadata(val proposal: ReferendumProposal)
}

fun ReferendumPreview.getName(): String? {
    return offChainMetadata?.title
        ?: getOnChainName()
}

private fun ReferendumPreview.getOnChainName(): String? {
    return when (val proposal = onChainMetadata?.proposal) {
        is ReferendumProposal.Call -> "${proposal.call.module.name}.${proposal.call.function.name}"
        else -> null
    }
}
