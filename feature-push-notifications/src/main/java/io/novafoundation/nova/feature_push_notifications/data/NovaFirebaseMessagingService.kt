package io.novafoundation.nova.feature_push_notifications.data

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureApi
import kotlinx.coroutines.tasks.await

class NovaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        getPushNotificationService().onTokenUpdated(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("NovaFirebaseMessagingService", "onMessageReceived: $message")
    }

    private fun getPushNotificationService(): PushNotificationsService {
        return FeatureUtils.getFeature<PushNotificationsFeatureApi>(this, PushNotificationsFeatureApi::class.java)
            .pushNotificationService()
    }

    companion object {

        suspend fun getToken(): String? {
            return runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
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
}
