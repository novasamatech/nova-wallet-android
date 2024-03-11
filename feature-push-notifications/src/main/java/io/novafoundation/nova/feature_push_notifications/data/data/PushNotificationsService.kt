package io.novafoundation.nova.feature_push_notifications.data.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.messaging
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.data.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.subscription.PushSubscriptionService
import kotlinx.coroutines.launch
import kotlin.jvm.Throws

const val PUSH_LOG_TAG = "NOVA_PUSH"

interface PushNotificationsService {

    fun onTokenUpdated(token: String)

    fun isPushNotificationsEnabled(): Boolean

    suspend fun initPushNotifications(): Result<Unit>

    suspend fun updatePushSettings(enabled: Boolean, pushSettings: PushSettings?): Result<Unit>

    fun isPushNotificationsAvailable(): Boolean

    suspend fun syncSettings()
}

class RealPushNotificationsService(
    private val context: Context,
    private val settingsProvider: PushSettingsProvider,
    private val subscriptionService: PushSubscriptionService,
    private val rootScope: RootScope,
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

    override suspend fun updatePushSettings(enabled: Boolean, pushSettings: PushSettings?): Result<Unit> {
        if (!googleApiAvailabilityProvider.isAvailable()) throw IllegalStateException("Google API is not available")

        return runCatching {
            handlePushTokenIfNeeded(enabled)
            val pushToken = getPushToken()
            val oldSettings = settingsProvider.getPushSettings()
            subscriptionService.handleSubscription(enabled, pushToken, oldSettings, pushSettings)
            settingsProvider.setPushNotificationsEnabled(enabled)
            settingsProvider.updateSettings(pushSettings)
        }
    }

    override fun isPushNotificationsAvailable(): Boolean {
        return googleApiAvailabilityProvider.isAvailable()
    }

    override suspend fun syncSettings() {
        if (!isPushNotificationsEnabled()) return

        val isPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        updatePushSettings(isPermissionGranted, settingsProvider.getPushSettings())
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return settingsProvider.isPushNotificationsEnabled()
    }

    override suspend fun initPushNotifications(): Result<Unit> {
        if (!googleApiAvailabilityProvider.isAvailable()) return Result.success(Unit)

        return updatePushSettings(true, settingsProvider.getDefaultPushSettings())
    }

    @Throws
    private suspend fun handlePushTokenIfNeeded(isEnable: Boolean) {
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

        skipTokenReceivingCallback = false
    }

    private fun getPushToken(): String? {
        return tokenCache.getPushToken()
    }

    private fun logToken() {
        if (!BuildConfig.DEBUG) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }

                Log.d(PUSH_LOG_TAG, "FCM token: ${task.result}")
            }
        )
    }
}
