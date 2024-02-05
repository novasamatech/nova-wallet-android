package io.novafoundation.nova.feature_push_notifications.data.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.repeatUntil
import io.novafoundation.nova.feature_push_notifications.data.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.sbscription.PushSubscriptionService
import kotlinx.coroutines.launch

private const val PREFS_NEED_TO_SYNC_TOKEN = "need_to_sync_token"

interface PushNotificationsService {

    fun onTokenUpdated(token: String)

    fun isNeedToSyncSettings(): Boolean

    suspend fun onSettingsUpdated(settings: PushSettings): Result<Unit>

    suspend fun syncSettings()
}

class RealPushNotificationsService(
    private val pushSettingsProvider: PushSettingsProvider,
    private val pushSubscriptionService: PushSubscriptionService,
    private val rootScope: RootScope,
    private val preferences: Preferences,
    private val pushTokenCache: PushTokenCache,
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
) : PushNotificationsService {

    init {
        NovaFirebaseMessagingService.logToken()
    }

    override fun onTokenUpdated(token: String) {
        if (!googleApiAvailabilityProvider.isAvailable()) return

        rootScope.launch {
            pushTokenCache.updatePushToken(token)

            syncSettings()
        }
    }

    override suspend fun syncSettings() {
        if (!googleApiAvailabilityProvider.isAvailable()) return

        val pushToken = getPushTokenOrFallback() ?: return
        syncSettingsInternal(pushToken)
    }

    override suspend fun onSettingsUpdated(settings: PushSettings): Result<Unit> {
        if (!googleApiAvailabilityProvider.isAvailable()) return Result.success(Unit)

        return runCatching {
            val pushToken = getPushTokenOrFallback() ?: throw IllegalStateException("Push token is not set")
            pushSettingsProvider.updateWalletSettings(settings)
            pushSubscriptionService.handleSubscription(pushToken, settings)
        }
    }

    override fun isNeedToSyncSettings(): Boolean {
        if (!googleApiAvailabilityProvider.isAvailable()) return false

        return preferences.getBoolean(PREFS_NEED_TO_SYNC_TOKEN, false)
    }

    private fun setNeedToSyncSettings(needToSync: Boolean) {
        preferences.putBoolean(PREFS_NEED_TO_SYNC_TOKEN, needToSync)
    }

    private suspend fun getPushTokenOrFallback(): String? {
        return pushTokenCache.getPushToken() ?: NovaFirebaseMessagingService.getToken()
    }

    private suspend fun syncSettingsInternal(token: String) {
        if (!googleApiAvailabilityProvider.isAvailable()) return

        val succesfullSync = repeatUntil(maxTimes = 5) {
            val pushSettings = pushSettingsProvider.getPushSettings() ?: PushSettings.getDefault()
            val result = runCatching {
                pushSubscriptionService.handleSubscription(token, pushSettings)
            }

            result.isSuccess
        }

        setNeedToSyncSettings(!succesfullSync)
    }
}
