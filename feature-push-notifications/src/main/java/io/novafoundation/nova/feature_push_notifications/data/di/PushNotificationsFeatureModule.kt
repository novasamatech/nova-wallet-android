package io.novafoundation.nova.feature_push_notifications.data.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.PushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.settings.LocalPushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.settings.RemotePushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.RealPushNotificationsInteractor

@Module()
class PushNotificationsFeatureModule {

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
    ): LocalPushSettingsProvider {
        return LocalPushSettingsProvider(gson, preferences)
    }

    @Provides
    @FeatureScope
    fun provideRemotePushSettingsProvider(
        pushTokenCache: PushTokenCache
    ): RemotePushSettingsProvider {
        return RemotePushSettingsProvider(pushTokenCache)
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsService(
        localPushSettingsProvider: LocalPushSettingsProvider,
        remotePushSettingsProvider: RemotePushSettingsProvider,
        rootScope: RootScope,
        preferences: Preferences,
        pushTokenCache: PushTokenCache
    ): PushNotificationsService {
        return RealPushNotificationsService(
            localPushSettingsProvider,
            remotePushSettingsProvider,
            rootScope,
            preferences,
            pushTokenCache
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
