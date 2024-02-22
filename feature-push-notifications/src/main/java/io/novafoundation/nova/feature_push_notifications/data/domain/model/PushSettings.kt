package io.novafoundation.nova.feature_push_notifications.data.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushSettings(
    val announcementsEnabled: Boolean,
    val sentTokensEnabled: Boolean,
    val receivedTokensEnabled: Boolean,
    val governanceState: List<GovernanceFeature>,
    val newReferenda: List<GovernanceFeature>,
    val subscribedMetaAccounts: Set<Long>,
    val stakingReward: ChainFeature,
    val govMyDelegatorVoted: ChainFeature,
    val govMyReferendumFinished: ChainFeature
) {

    class GovernanceFeature(val chainId: ChainId, val tracks: List<String>)

    sealed class ChainFeature {

        object All : ChainFeature()

        data class Concrete(val chainIds: List<ChainId>) : ChainFeature()
    }
}

fun PushSettings.ChainFeature.isNotEmpty(): Boolean {
    return when (this) {
        is PushSettings.ChainFeature.All -> false
        is PushSettings.ChainFeature.Concrete -> chainIds.isNotEmpty()
    }
}

fun PushSettings.isAnyGovEnabled(): Boolean {
    return governanceState.isNotEmpty() ||
        newReferenda.isNotEmpty() ||
        govMyDelegatorVoted.isNotEmpty() ||
        govMyReferendumFinished.isNotEmpty()
}
