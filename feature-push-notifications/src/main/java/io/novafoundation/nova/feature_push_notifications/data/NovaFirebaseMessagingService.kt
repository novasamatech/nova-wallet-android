package io.novafoundation.nova.feature_push_notifications.data

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureComponent

class NovaFirebaseMessagingService : FirebaseMessagingService() {

    private val pushNotificationService = FeatureUtils.getFeature<PushNotificationsFeatureComponent>(
        this,
        PushNotificationsFeatureApi::class.java
    )
        .getPushNotificationService()

    override fun onNewToken(token: String) {
        pushNotificationService.onTokenUpdated(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("NovaFirebaseMessagingService", "onMessageReceived: $message")
    }

    companion object {

        fun logToken() {
            if (!BuildConfig.DEBUG) return
            
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }

                Log.d("NovaFirebaseMessagingService", "FCM token: ${task.result}")
            })
        }
    }
}
