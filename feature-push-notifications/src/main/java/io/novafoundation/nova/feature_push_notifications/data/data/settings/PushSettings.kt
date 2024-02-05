package io.novafoundation.nova.feature_push_notifications.data.data.settings

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushSettings(
    val appMajorUpdates: Boolean,
    val appCriticalUpdates: Boolean,
    val chainReferendums: Boolean,
    val wallets: List<Wallet>
) {

    data class Wallet(
        val baseEthereumAccount: String,
        val baseSubstrateAccount: String,
        val chainAccounts: Map<ChainId, String>,
        val notifications: Notifications
    )

    data class Notifications(
        val stakingReward: ChainFeature,
        val transfer: ChainFeature
    )

    sealed class ChainFeature {

        object All : ChainFeature()

        data class Concrete(val chainIds: List<String>) : ChainFeature()
    }

    companion object {

        fun getDefault(): PushSettings {
            return PushSettings(
                appMajorUpdates = false,
                appCriticalUpdates = false,
                chainReferendums = false,
                wallets = emptyList()
            )
        }
    }
}
