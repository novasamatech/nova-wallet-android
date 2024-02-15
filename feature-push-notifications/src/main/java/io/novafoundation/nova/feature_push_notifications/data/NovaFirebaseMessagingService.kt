package io.novafoundation.nova.feature_push_notifications.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureComponent
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class NovaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushNotificationsService: PushNotificationsService

    init {
        injectDependencies()
    }

    override fun onNewToken(token: String) {
        pushNotificationsService.onTokenUpdated(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("NovaFirebaseMessagingService", "onMessageReceived: $message")
    }

    private fun injectDependencies() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(this, PushNotificationsFeatureApi::class.java)
            .inject(this)
    }

    companion object {

        suspend fun getToken(): String? {
            return runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        }
    }
}
