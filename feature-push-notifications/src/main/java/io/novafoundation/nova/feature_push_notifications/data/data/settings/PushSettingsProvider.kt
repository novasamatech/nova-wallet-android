package io.novafoundation.nova.feature_push_notifications.data.data.settings

interface PushSettingsProvider {

    suspend fun getWalletSettings(): PushWalletSettings?

    suspend fun updateWalletSettings(pushWalletSettings: PushWalletSettings)

}

data class PushWalletSettings(
    val pushToken: String,
    val updatedAt: String,
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

        fun getDefault(token: String, updatedAt: String): PushWalletSettings {
            return PushWalletSettings(
                pushToken = token,
                updatedAt = updatedAt,
                wallets = emptyList()
            )
        }
    }
}
