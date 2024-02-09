package io.novafoundation.nova.feature_push_notifications.data.data.settings

import kotlinx.coroutines.flow.Flow

interface PushSettingsProvider {

    suspend fun getPushSettings(): PushSettings?

    suspend fun updateWalletSettings(pushWalletSettings: PushSettings)

    fun setPushNotificationsEnabled(isEnabled: Boolean)

    fun isPushNotificationsEnabled(): Boolean

    fun pushEnabledFlow(): Flow<Boolean>
}
