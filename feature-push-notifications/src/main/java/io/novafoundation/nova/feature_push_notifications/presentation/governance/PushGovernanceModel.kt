package io.novafoundation.nova.feature_push_notifications.presentation.governance

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushGovernanceModel(
    val chainId: ChainId,
    val governance: Chain.Governance,
    val chainName: String,
    val chainIconUrl: String,
    val isEnabled: Boolean,
    val isNewReferendaEnabled: Boolean,
    val isReferendaUpdatesEnabled: Boolean,
    val trackIds: Set<TrackId>
) {
    companion object
}

fun PushGovernanceModel.Companion.default(
    chain: Chain,
    governance: Chain.Governance,
    tracks: Set<TrackId>
): PushGovernanceModel {
    return PushGovernanceModel(
        chainId = chain.id,
        governance = governance,
        chainName = chain.name,
        chainIconUrl = chain.icon,
        false,
        isNewReferendaEnabled = true,
        isReferendaUpdatesEnabled = true,
        trackIds = tracks
    )
}
