package io.novafoundation.nova.feature_push_notifications.domain.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushSettings(
    val announcementsEnabled: Boolean,
    val sentTokensEnabled: Boolean,
    val receivedTokensEnabled: Boolean,
    val multisigTransactionsEnabled: Boolean,
    val subscribedMetaAccounts: Set<Long>,
    val stakingReward: ChainFeature,
    val governance: Map<ChainId, GovernanceState>
) {

    data class GovernanceState(
        val newReferendaEnabled: Boolean,
        val referendumUpdateEnabled: Boolean,
        val govMyDelegateVotedEnabled: Boolean,
        val tracks: Set<TrackId>
    )

    sealed class ChainFeature {

        object All : ChainFeature()

        data class Concrete(val chainIds: List<ChainId>) : ChainFeature()
    }

    fun settingsIsEmpty(): Boolean {
        return !announcementsEnabled &&
            !sentTokensEnabled &&
            !receivedTokensEnabled &&
            stakingReward.isEmpty() &&
            !isGovEnabled()
    }
}

fun PushSettings.ChainFeature.isEmpty(): Boolean {
    return when (this) {
        is PushSettings.ChainFeature.All -> false
        is PushSettings.ChainFeature.Concrete -> chainIds.isEmpty()
    }
}

fun PushSettings.ChainFeature.isNotEmpty(): Boolean {
    return !isEmpty()
}

fun PushSettings.isGovEnabled(): Boolean {
    return governance.values.any {
        (it.newReferendaEnabled || it.referendumUpdateEnabled || it.govMyDelegateVotedEnabled) && it.tracks.isNotEmpty()
    }
}
