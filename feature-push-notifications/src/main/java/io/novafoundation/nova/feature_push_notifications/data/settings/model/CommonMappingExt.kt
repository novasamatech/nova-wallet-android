package io.novafoundation.nova.feature_push_notifications.data.settings.model

import io.novafoundation.nova.feature_push_notifications.data.settings.model.chain.ChainFeatureCacheV1
import io.novafoundation.nova.feature_push_notifications.data.settings.model.governance.GovernanceStateCacheV1
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings

fun PushSettings.toCache(): PushSettingsCacheV2 {
    return PushSettingsCacheV2(
        announcementsEnabled = announcementsEnabled,
        sentTokensEnabled = sentTokensEnabled,
        receivedTokensEnabled = receivedTokensEnabled,
        multisigTransactionsEnabled = multisigTransactionsEnabled,
        subscribedMetaAccounts = subscribedMetaAccounts,
        stakingReward = stakingReward.toCache(),
        governance = governance.mapValues { (_, value) -> value.toCache() }
    )
}

fun PushSettings.ChainFeature.toCache(): ChainFeatureCacheV1 {
    return when (this) {
        is PushSettings.ChainFeature.All -> ChainFeatureCacheV1.All
        is PushSettings.ChainFeature.Concrete -> ChainFeatureCacheV1.Concrete(chainIds)
    }
}

fun PushSettings.GovernanceState.toCache(): GovernanceStateCacheV1 {
    return GovernanceStateCacheV1(
        newReferendaEnabled = newReferendaEnabled,
        referendumUpdateEnabled = referendumUpdateEnabled,
        govMyDelegateVotedEnabled = govMyDelegateVotedEnabled,
        tracks = tracks
    )
}
