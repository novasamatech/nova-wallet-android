package io.novafoundation.nova.feature_push_notifications.data.data.settings.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class PushSettingsCacheV1(
    val announcementsEnabled: Boolean,
    val sentTokensEnabled: Boolean,
    val receivedTokensEnabled: Boolean,
    val subscribedMetaAccounts: Set<Long>,
    val stakingReward: ChainFeature,
    val governance: Map<ChainId, GovernanceState>
) : PushSettingsCache {

    override val version: String = "V1"

    override fun toPushSettings(): PushSettings {
        return PushSettings(
            announcementsEnabled = announcementsEnabled,
            sentTokensEnabled = sentTokensEnabled,
            receivedTokensEnabled = receivedTokensEnabled,
            subscribedMetaAccounts = subscribedMetaAccounts,
            stakingReward = stakingReward.toDomain(),
            governance = governance.mapValues { (_, value) -> value.toDomain() }
        )
    }

    class GovernanceState(
        val newReferendaEnabled: Boolean,
        val referendumUpdateEnabled: Boolean,
        val govMyDelegateVotedEnabled: Boolean,
        val tracks: Set<TrackId>
    )

    sealed class ChainFeature {

        object All : ChainFeature()

        data class Concrete(val chainIds: List<ChainId>) : ChainFeature()
    }
}

fun PushSettings.toCache(): PushSettingsCacheV1 {
    return PushSettingsCacheV1(
        announcementsEnabled = announcementsEnabled,
        sentTokensEnabled = sentTokensEnabled,
        receivedTokensEnabled = receivedTokensEnabled,
        subscribedMetaAccounts = subscribedMetaAccounts,
        stakingReward = stakingReward.toCache(),
        governance = governance.mapValues { (_, value) -> value.toCache() }
    )
}

fun PushSettings.ChainFeature.toCache(): PushSettingsCacheV1.ChainFeature {
    return when (this) {
        is PushSettings.ChainFeature.All -> PushSettingsCacheV1.ChainFeature.All
        is PushSettings.ChainFeature.Concrete -> PushSettingsCacheV1.ChainFeature.Concrete(chainIds)
    }
}

fun PushSettingsCacheV1.ChainFeature.toDomain(): PushSettings.ChainFeature {
    return when (this) {
        is PushSettingsCacheV1.ChainFeature.All -> PushSettings.ChainFeature.All
        is PushSettingsCacheV1.ChainFeature.Concrete -> PushSettings.ChainFeature.Concrete(chainIds)
    }
}

fun PushSettingsCacheV1.GovernanceState.toDomain(): PushSettings.GovernanceState {
    return PushSettings.GovernanceState(
        newReferendaEnabled = newReferendaEnabled,
        referendumUpdateEnabled = referendumUpdateEnabled,
        govMyDelegateVotedEnabled = govMyDelegateVotedEnabled,
        tracks = tracks
    )
}

fun PushSettings.GovernanceState.toCache(): PushSettingsCacheV1.GovernanceState {
    return PushSettingsCacheV1.GovernanceState(
        newReferendaEnabled = newReferendaEnabled,
        referendumUpdateEnabled = referendumUpdateEnabled,
        govMyDelegateVotedEnabled = govMyDelegateVotedEnabled,
        tracks = tracks
    )
}
