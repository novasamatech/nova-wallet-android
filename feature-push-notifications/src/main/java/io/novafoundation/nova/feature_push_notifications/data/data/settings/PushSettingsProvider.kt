package io.novafoundation.nova.feature_push_notifications.data.data.settings

import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings

interface PushSettingsProvider {

    suspend fun getPushSettings(): PushSettings?

    suspend fun updateWalletSettings(pushWalletSettings: PushSettings)

}
