package io.novafoundation.nova.feature_push_notifications.data.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.formatting.formatDateISO_8601
import io.novafoundation.nova.common.utils.repeatUntil
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.data.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.data.data.settings.LocalPushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushWalletSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.RemotePushSettingsProvider
import java.util.*
import kotlinx.coroutines.launch

private const val PREFS_NEED_TO_SYNC_TOKEN = "need_to_sync_token"

interface PushNotificationsService {
    fun onTokenUpdated(token: String)

    fun isNeedToSyncSettings(): Boolean

    suspend fun onSettingsUpdated(walletSettings: PushWalletSettings)

    suspend fun syncSettings()
}

class RealPushNotificationsService(
    private val localPushSettingsProvider: LocalPushSettingsProvider,
    private val remotePushSettingsProvider: RemotePushSettingsProvider,
    private val rootScope: RootScope,
    private val preferences: Preferences,
    private val pushTokenCache: PushTokenCache
) : PushNotificationsService {

    init {
        NovaFirebaseMessagingService.logToken()
    }

    override fun onTokenUpdated(token: String) {
        rootScope.launch {
            pushTokenCache.updatePushToken(token)
            updateLocalPushSettings(token)

            syncSettings()
        }
    }

    override suspend fun syncSettings() {
        val succesfullSync = repeatUntil(maxTimes = 5) {
            val result = runCatching {
                val walletSettings = localPushSettingsProvider.getWalletSettings() ?: return@syncSettings
                remotePushSettingsProvider.updateWalletSettings(walletSettings)
            }

            result.isSuccess
        }

        setNeedToSyncSettings(!succesfullSync)
    }

    override suspend fun onSettingsUpdated(walletSettings: PushWalletSettings) {
        localPushSettingsProvider.updateWalletSettings(walletSettings)
        remotePushSettingsProvider.updateWalletSettings(walletSettings)
    }

    override fun isNeedToSyncSettings(): Boolean {
        return preferences.getBoolean(PREFS_NEED_TO_SYNC_TOKEN, false)
    }

    private fun setNeedToSyncSettings(needToSync: Boolean) {
        preferences.putBoolean(PREFS_NEED_TO_SYNC_TOKEN, needToSync)
    }

    private suspend fun updateLocalPushSettings(token: String) {
        val walletSettings = localPushSettingsProvider.getWalletSettings()
            ?: PushWalletSettings.getDefault(token, formatDateISO_8601(Date()))
        localPushSettingsProvider.updateWalletSettings(walletSettings.copy(pushToken = token))
    }
}
