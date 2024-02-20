package io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushGovernanceRVItem(
    val chainId: ChainId,
    val governance: Chain.Governance,
    val chainName: String,
    val chainIconUrl: String,
    val isEnabled: Boolean,
    val isNewReferendaEnabled: Boolean,
    val isReferendaUpdatesEnabled: Boolean,
    val isDelegationVotesEnabled: Boolean,
    val tracks: Tracks
) {

    sealed class Tracks {
        object All : Tracks()

        data class Specified(val items: List<String>, val max: Int) : Tracks()
    }

    companion object
}

fun PushGovernanceRVItem.Companion.default(chain: Chain, governance: Chain.Governance): PushGovernanceRVItem {
    return PushGovernanceRVItem(
        chain.id,
        governance,
        chain.name,
        chain.icon,
        false,
        true,
        true,
        true,
        PushGovernanceRVItem.Tracks.All
    )
}
