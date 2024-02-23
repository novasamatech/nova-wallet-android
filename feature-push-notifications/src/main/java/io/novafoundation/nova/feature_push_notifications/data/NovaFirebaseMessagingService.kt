package io.novafoundation.nova.feature_push_notifications.data

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureComponent
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationHandler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.CoroutineContext

class NovaFirebaseMessagingService : FirebaseMessagingService(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    @Inject
    lateinit var pushNotificationsService: PushNotificationsService

    @Inject
    lateinit var notificationHandler: NotificationHandler

    override fun onCreate() {
        super.onCreate()

        injectDependencies()
    }

    override fun onNewToken(token: String) {
        pushNotificationsService.onTokenUpdated(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        launch {
            notificationHandler.handleNotification(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineContext.cancel()
    }

    private fun injectDependencies() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(this, PushNotificationsFeatureApi::class.java)
            .inject(this)
    }

    companion object {

        suspend fun getToken(): String? {
            return runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        }

        suspend fun requestToken(): String {
            return FirebaseMessaging.getInstance().token.await()
        }

        suspend fun deleteToken() {
            FirebaseMessaging.getInstance().deleteToken()
        }
    }
}
