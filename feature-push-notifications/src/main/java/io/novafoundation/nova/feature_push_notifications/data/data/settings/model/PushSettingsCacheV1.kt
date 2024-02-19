package io.novafoundation.nova.feature_push_notifications.data.data.settings.model

import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class PushSettingsCacheV1(
    val announcementsEnabled: Boolean,
    val sentTokensEnabled: Boolean,
    val receivedTokensEnabled: Boolean,
    val governanceState: List<GovernanceFeature>,
    val newReferenda: List<GovernanceFeature>,
    val wallets: List<Wallet>,
    val stakingReward: ChainFeature,
    val govMyDelegatorVoted: ChainFeature,
    val govMyReferendumFinished: ChainFeature
) : PushSettingsCache {

    override val version: String = "V1"

    override fun toPushSettings(): PushSettings {
        return PushSettings(
            announcementsEnabled = announcementsEnabled,
            sentTokensEnabled = sentTokensEnabled,
            receivedTokensEnabled = receivedTokensEnabled,
            governanceState = governanceState.map { PushSettings.GovernanceFeature(it.chainId, it.tracks) },
            newReferenda = newReferenda.map { PushSettings.GovernanceFeature(it.chainId, it.tracks) },
            wallets = wallets.map {
                PushSettings.Wallet(
                    baseEthereumAccount = it.baseEthereumAccount,
                    baseSubstrateAccount = it.baseSubstrateAccount,
                    chainAccounts = it.chainAccounts.mapValues { (_, chainAccount) -> chainAccount }
                )
            },
            stakingReward = stakingReward.toDomain(),
            govMyDelegatorVoted = govMyDelegatorVoted.toDomain(),
            govMyReferendumFinished = govMyReferendumFinished.toDomain()
        )
    }

    data class Wallet(
        val baseEthereumAccount: ByteArray?,
        val baseSubstrateAccount: ByteArray?,
        val chainAccounts: Map<ChainId, ByteArray>
    )

    data class GovernanceFeature(val chainId: ChainId, val tracks: List<String>)

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
        governanceState = governanceState.map { PushSettingsCacheV1.GovernanceFeature(it.chainId, it.tracks) },
        newReferenda = newReferenda.map { PushSettingsCacheV1.GovernanceFeature(it.chainId, it.tracks) },
        wallets = wallets.map { it.toCache() },
        stakingReward = stakingReward.toCache(),
        govMyDelegatorVoted = govMyDelegatorVoted.toCache(),
        govMyReferendumFinished = govMyReferendumFinished.toCache()
    )
}

fun PushSettings.Wallet.toCache(): PushSettingsCacheV1.Wallet {
    return PushSettingsCacheV1.Wallet(
        baseEthereumAccount = baseEthereumAccount,
        baseSubstrateAccount = baseSubstrateAccount,
        chainAccounts = chainAccounts.mapValues { (_, chainAccount) -> chainAccount }
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
