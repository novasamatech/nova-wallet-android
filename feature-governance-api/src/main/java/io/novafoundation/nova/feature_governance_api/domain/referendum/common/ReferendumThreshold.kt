package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold.Threshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.merge
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

data class ReferendumThreshold(
    val support: Threshold<Balance>,
    val approval: Threshold<Perbill>
)

fun Threshold<*>.currentlyPassing(): Boolean {
    return currentlyPassing
}

fun ReferendumThreshold.currentlyPassing(): Boolean {
    return support.currentlyPassing() && approval.currentlyPassing()
}

fun ReferendumThreshold.projectedPassing(): VotingThreshold.ProjectedPassing {
    return support.projectedPassing.merge(approval.projectedPassing)
}
