package io.novafoundation.nova.feature_push_notifications.data.data.settings

data class PushSettings(
    val appMajorUpdates: Boolean,
    val appCriticalUpdates: Boolean,
    val chainReferendums: Boolean,
    val wallets: List<Wallet>
) {

    data class Wallet(
        val baseEthereumAccount: String,
        val baseSubstrateAccount: String,
        val chainAccounts: Map<String, String>,
        val notifications: Notifications
    )

    data class Notifications(
        val stakingReward: StakingReward,
        val transfer: Transfer
    )

    data class StakingReward(
        val type: String
    )

    data class Transfer(
        val type: String,
        val value: List<String>
    )

    companion object {

        fun getDefault(): PushSettings {
            return PushSettings(
                appMajorUpdates = true,
                appCriticalUpdates = true,
                chainReferendums = true,
                wallets = emptyList()
            )
        }
    }
}
