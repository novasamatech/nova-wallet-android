package io.novafoundation.nova.feature_push_notifications.data.data

import com.google.firebase.messaging.messaging
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.data.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.subscription.PushSubscriptionService
import kotlinx.coroutines.launch
import kotlin.jvm.Throws

interface PushNotificationsService {

    fun onTokenUpdated(token: String)

    fun isPushNotificationsEnabled(): Boolean

    suspend fun initPushNotifications(): Result<Unit>

    suspend fun updatePushSettings(enabled: Boolean, pushSettings: PushSettings): Result<Unit>
}

class RealPushNotificationsService(
    private val settingsProvider: PushSettingsProvider,
    private val subscriptionService: PushSubscriptionService,
    private val rootScope: RootScope,
    private val preferences: Preferences,
    private val tokenCache: PushTokenCache,
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
) : PushNotificationsService {

    // Using to manually sync subscriptions (firestore, topics) after enabling push notifications
    private var skipTokenReceivingCallback = false

    init {
        if (isPushNotificationsEnabled()) {
            logToken()
        }
    }

    override fun onTokenUpdated(token: String) {
        if (!googleApiAvailabilityProvider.isAvailable()) return
        if (!isPushNotificationsEnabled()) return
        if (skipTokenReceivingCallback) return

        logToken()

        rootScope.launch {
            tokenCache.updatePushToken(token)
            updatePushSettings(isPushNotificationsEnabled(), settingsProvider.getPushSettings())
        }
    }

    override suspend fun updatePushSettings(enabled: Boolean, pushSettings: PushSettings): Result<Unit> {
        if (!googleApiAvailabilityProvider.isAvailable()) throw IllegalStateException("Google API is not available")

        return runCatching {
            setPushNotificationsEnabled(enabled)
            val pushToken = getPushToken()
            val oldSettings = settingsProvider.getPushSettings()
            subscriptionService.handleSubscription(enabled, pushToken, oldSettings, pushSettings)
            settingsProvider.updateSettings(pushSettings)
        }
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return settingsProvider.isPushNotificationsEnabled()
    }

    override suspend fun initPushNotifications(): Result<Unit> {
        if (!googleApiAvailabilityProvider.isAvailable()) return Result.success(Unit)

        return updatePushSettings(true, settingsProvider.getDefaultPushSettings())
    }

    @Throws
    private suspend fun setPushNotificationsEnabled(isEnable: Boolean) {
        if (isEnable == isPushNotificationsEnabled()) return
        skipTokenReceivingCallback = true

        val pushToken = if (isEnable) {
            NovaFirebaseMessagingService.requestToken()
        } else {
            NovaFirebaseMessagingService.deleteToken()
            null
        }

        tokenCache.updatePushToken(pushToken)
        Firebase.messaging.isAutoInitEnabled = isEnable
        settingsProvider.setPushNotificationsEnabled(isEnable)

        skipTokenReceivingCallback = false
    }

    private suspend fun getPushToken(): String? {
        return tokenCache.getPushToken()
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
