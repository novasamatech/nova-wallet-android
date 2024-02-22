package io.novafoundation.nova.feature_push_notifications.data.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.CompoundNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.DefaultNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.SendingNotificationHandler

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
