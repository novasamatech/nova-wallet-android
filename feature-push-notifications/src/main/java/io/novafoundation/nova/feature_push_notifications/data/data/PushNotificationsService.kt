package io.novafoundation.nova.feature_push_notifications.data.data

import com.google.firebase.messaging.messaging
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.repeatUntil
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.data.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.subscription.PushSubscriptionService
import kotlinx.coroutines.launch

private const val PREFS_NEED_TO_SYNC_TOKEN = "need_to_sync_token"

interface PushNotificationsService {

    fun onTokenUpdated(token: String)

    fun isNeedToSyncSettings(): Boolean

    fun isPushNotificationsEnabled(): Boolean

    suspend fun setPushNotificationsEnabled(isEnabled: Boolean): Result<Unit>

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
        if (isPushNotificationsEnabled()) {
            logToken()
        }
    }

    override fun onTokenUpdated(token: String) {
        if (!googleApiAvailabilityProvider.isAvailable()) return
        if (!isPushNotificationsEnabled()) return

        logToken()

        rootScope.launch {
            pushTokenCache.updatePushToken(token)

            syncSettings()
        }
    }

    override suspend fun syncSettings() {
        if (!googleApiAvailabilityProvider.isAvailable()) return
        if (!isPushNotificationsEnabled()) return

        val pushToken = getPushTokenOrFallback() ?: return
        syncSettingsInternal(pushToken)
    }

    override suspend fun onSettingsUpdated(settings: PushSettings): Result<Unit> {
        if (!googleApiAvailabilityProvider.isAvailable()) throw IllegalStateException("Google API is not available")

        return runCatching {
            if (!isPushNotificationsEnabled()) throw IllegalStateException("Push notifications are not enabled")

            val pushToken = getPushTokenOrFallback() ?: throw IllegalStateException("Push token is not set")
            pushSettingsProvider.updateWalletSettings(settings)
            pushSubscriptionService.handleSubscription(pushToken, settings)
        }
    }

    override fun isNeedToSyncSettings(): Boolean {
        if (!googleApiAvailabilityProvider.isAvailable()) return false
        if (!isPushNotificationsEnabled()) return false

        return preferences.getBoolean(PREFS_NEED_TO_SYNC_TOKEN, false)
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return pushSettingsProvider.isPushNotificationsEnabled()
    }

    override suspend fun setPushNotificationsEnabled(isEnabled: Boolean): Result<Unit> {
        val result = if (isEnabled) {
            NovaFirebaseMessagingService.requestToken()
        } else {
            NovaFirebaseMessagingService.deleteToken()
        }

        result.onSuccess {
            Firebase.messaging.isAutoInitEnabled = isEnabled
            pushSettingsProvider.setPushNotificationsEnabled(isEnabled)
        }

        return result
    }

    private fun setNeedToSyncSettings(needToSync: Boolean) {
        preferences.putBoolean(PREFS_NEED_TO_SYNC_TOKEN, needToSync)
    }

    private suspend fun getPushTokenOrFallback(): String? {
        return pushTokenCache.getPushToken() ?: NovaFirebaseMessagingService.getToken()
    }

    private suspend fun syncSettingsInternal(token: String) {
        if (!googleApiAvailabilityProvider.isAvailable()) return
        if (!isPushNotificationsEnabled()) return

        val succesfullSync = repeatUntil(maxTimes = 5) {
            val pushSettings = pushSettingsProvider.getPushSettings() ?: PushSettings.getDefault()
            val result = runCatching {
                pushSubscriptionService.handleSubscription(token, pushSettings)
            }

            result.isSuccess
        }

        setNeedToSyncSettings(!succesfullSync)
    }

    fun logToken() {
        if (!BuildConfig.DEBUG) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }

                Log.d("NOVA_PUSH_TOKEN", "FCM token: ${task.result}")
            }
        )
    }
}
