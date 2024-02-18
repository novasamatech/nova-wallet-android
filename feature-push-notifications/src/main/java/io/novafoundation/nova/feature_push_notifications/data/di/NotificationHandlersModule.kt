package io.novafoundation.nova.feature_push_notifications.data.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_push_notifications.data.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.PushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsSerializer
import io.novafoundation.nova.feature_push_notifications.data.data.settings.RealPushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.subscription.PushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.data.subscription.RealPushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.RealPushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.RealWelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.CompoundNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.DefaultNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.SendingNotificationHandler
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Module()
class NotificationHandlersModule {

    @Provides
    fun provideNotificationManagerCompat(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

    @Provides
    @IntoSet
    fun defaultNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager
    ): NotificationHandler {
        return DefaultNotificationHandler(context, notificationManagerCompat, resourceManager)
    }

    @Provides
    @IntoSet
    fun sendingNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager
    ): NotificationHandler {
        return SendingNotificationHandler(context, notificationManagerCompat, resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideCompoundnotificationHandler(
        handlers: Set<@JvmSuppressWildcards NotificationHandler>
    ): NotificationHandler {
        return CompoundNotificationHandler(handlers)
    }
}
