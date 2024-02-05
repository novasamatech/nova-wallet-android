package io.novafoundation.nova.feature_push_notifications.data.data.settings

interface PushSettingsProvider {

    suspend fun getPushSettings(): PushSettings?

    suspend fun updateWalletSettings(pushWalletSettings: PushSettings)
}
