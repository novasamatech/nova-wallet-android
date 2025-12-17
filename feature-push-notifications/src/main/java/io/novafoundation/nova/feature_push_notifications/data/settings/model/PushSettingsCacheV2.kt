package io.novafoundation.nova.feature_push_notifications.data.settings.model

import io.novafoundation.nova.feature_push_notifications.data.settings.model.chain.ChainFeatureCacheV1
import io.novafoundation.nova.feature_push_notifications.data.settings.model.chain.toDomain
import io.novafoundation.nova.feature_push_notifications.data.settings.model.governance.GovernanceStateCacheV1
import io.novafoundation.nova.feature_push_notifications.data.settings.model.governance.MultisigsStateCacheV1
import io.novafoundation.nova.feature_push_notifications.data.settings.model.governance.toDomain
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class PushSettingsCacheV2(
    val announcementsEnabled: Boolean,
    val sentTokensEnabled: Boolean,
    val receivedTokensEnabled: Boolean,
    val subscribedMetaAccounts: Set<Long>,
    val stakingReward: ChainFeatureCacheV1,
    val governance: Map<ChainId, GovernanceStateCacheV1>,
    val multisigs: MultisigsStateCacheV1
) : PushSettingsCache {

    companion object {
        const val VERSION = "V2"
    }

    override val version: String = VERSION

    override fun toPushSettings(): PushSettings {
        return PushSettings(
            announcementsEnabled = announcementsEnabled,
            sentTokensEnabled = sentTokensEnabled,
            receivedTokensEnabled = receivedTokensEnabled,
            subscribedMetaAccounts = subscribedMetaAccounts,
            stakingReward = stakingReward.toDomain(),
            governance = governance.mapValues { (_, value) -> value.toDomain() },
            multisigs = multisigs.toDomain()
        )
    }
}
