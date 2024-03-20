package io.novafoundation.nova.feature_push_notifications.data

import com.google.firebase.messaging.messaging
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.domain.model.PushSettings
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.subscription.PushSubscriptionService
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val PUSH_LOG_TAG = "NOVA_PUSH"
private const val PREFS_LAST_SYNC_TIME = "PREFS_LAST_SYNC_TIME"
private const val MIN_DAYS_TO_START_SYNC = 1
private val SAVING_TIMEOUT = 15.seconds

interface PushNotificationsService {

    fun onTokenUpdated(token: String)

    fun isPushNotificationsEnabled(): Boolean

    suspend fun initPushNotifications(): Result<Unit>

    suspend fun updatePushSettings(enabled: Boolean, pushSettings: PushSettings?): Result<Unit>

    fun isPushNotificationsAvailable(): Boolean

    suspend fun syncSettingsIfNeeded()
}

class RealPushNotificationsService(
    private val settingsProvider: PushSettingsProvider,
    private val subscriptionService: PushSubscriptionService,
    private val rootScope: RootScope,
    private val tokenCache: PushTokenCache,
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
    private val pushPermissionRepository: PushPermissionRepository,
    private val preferences: Preferences
) : PushNotificationsService {

    // Using to manually sync subscriptions (firestore, topics) after enabling push notifications
    private var skipTokenReceivingCallback = false

    init {
        logToken()
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
        if (!googleApiAvailabilityProvider.isAvailable()) return googleApiFailureResult()

        return runCatching {
            withTimeout(SAVING_TIMEOUT) {
                handlePushTokenIfNeeded(enabled)
                val pushToken = getPushToken()
                val oldSettings = settingsProvider.getPushSettings()
                subscriptionService.handleSubscription(enabled, pushToken, oldSettings, pushSettings)
                settingsProvider.setPushNotificationsEnabled(enabled)
                settingsProvider.updateSettings(pushSettings)
                updateLastSyncTime()
            }
        }
    }

    override fun isPushNotificationsAvailable(): Boolean {
        return googleApiAvailabilityProvider.isAvailable()
    }

    override suspend fun syncSettingsIfNeeded() {
        if (!isPushNotificationsEnabled()) return
        if (!googleApiAvailabilityProvider.isAvailable()) return

        if (isPermissionsRevoked() || isTimeToSync()) {
            val isPermissionGranted = pushPermissionRepository.isPermissionGranted()
            updatePushSettings(isPermissionGranted, settingsProvider.getPushSettings())
        }
    }

    override fun isPushNotificationsEnabled(): Boolean {
        return settingsProvider.isPushNotificationsEnabled()
    }

    override suspend fun initPushNotifications(): Result<Unit> {
        if (!googleApiAvailabilityProvider.isAvailable()) return googleApiFailureResult()

        return updatePushSettings(true, settingsProvider.getDefaultPushSettings())
    }

    private suspend fun handlePushTokenIfNeeded(isEnable: Boolean) {
        if (!googleApiAvailabilityProvider.isAvailable()) return
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
        if (!isPushNotificationsEnabled()) return
        if (!BuildConfig.DEBUG) return
        if (!googleApiAvailabilityProvider.isAvailable()) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }

                Log.d(PUSH_LOG_TAG, "FCM token: ${task.result}")
            }
        )
    }

    private fun isPermissionsRevoked(): Boolean {
        return !pushPermissionRepository.isPermissionGranted()
    }

    private fun isTimeToSync(): Boolean {
        if (!isPushNotificationsEnabled()) return false

        val lastSyncTime = getLastSyncTimeIfPushEnabled()
        val deltaTimeBetweenNowAndLastSync = System.currentTimeMillis() - lastSyncTime
        val wholeDays = deltaTimeBetweenNowAndLastSync.milliseconds.inWholeDays
        return wholeDays >= MIN_DAYS_TO_START_SYNC
    }

    private fun updateLastSyncTime() {
        preferences.putLong(PREFS_LAST_SYNC_TIME, System.currentTimeMillis())
    }

    private fun getLastSyncTimeIfPushEnabled(): Long {
        return preferences.getLong(PREFS_LAST_SYNC_TIME, 0)
    }

    private fun googleApiFailureResult(): Result<Unit> {
        return Result.failure(IllegalStateException("Google API is not available"))
    }
}
