package io.novafoundation.nova.feature_push_notifications.data.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_push_notifications.data.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.PushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.settings.RealPushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.sbscription.PushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.data.sbscription.RealPushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.RealPushNotificationsInteractor

@Module()
class PushNotificationsFeatureModule {

    @Provides
    @FeatureScope
    fun providePushTokenCache(
        context: Context
    ): GoogleApiAvailabilityProvider {
        return GoogleApiAvailabilityProvider(context)
    }

    @Provides
    @FeatureScope
    fun providePushTokenCache(
        preferences: Preferences
    ): PushTokenCache {
        return RealPushTokenCache(preferences)
    }

    @Provides
    @FeatureScope
    fun provideLocalPushSettingsProvider(
        gson: Gson,
        preferences: Preferences
    ): RealPushSettingsProvider {
        return RealPushSettingsProvider(gson, preferences)
    }

    @Provides
    @FeatureScope
    fun provideRemotePushSettingsProvider(
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
    ): PushSubscriptionService {
        return RealPushSubscriptionService(googleApiAvailabilityProvider)
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsService(
        localPushSettingsProvider: RealPushSettingsProvider,
        pushSubscriptionService: PushSubscriptionService,
        rootScope: RootScope,
        preferences: Preferences,
        pushTokenCache: PushTokenCache,
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
    ): PushNotificationsService {
        return RealPushNotificationsService(
            localPushSettingsProvider,
            pushSubscriptionService,
            rootScope,
            preferences,
            pushTokenCache,
            googleApiAvailabilityProvider
        )
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsInteractor(
        pushNotificationsService: PushNotificationsService
    ): PushNotificationsInteractor {
        return RealPushNotificationsInteractor(pushNotificationsService)
    }
}
