package io.novafoundation.nova.feature_push_notifications.data.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
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
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PushSettingsSerialization

@Module(includes = [NotificationHandlersModule::class])
class PushNotificationsFeatureModule {

    @Provides
    @FeatureScope
    fun provideGoogleApiAvailabilityProvider(
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
    @PushSettingsSerialization
    fun providePushSettingsGson() = PushSettingsSerializer.gson()

    @Provides
    @FeatureScope
    fun providePushSettingsProvider(
        @PushSettingsSerialization gson: Gson,
        preferences: Preferences,
        accountRepository: AccountRepository
    ): PushSettingsProvider {
        return RealPushSettingsProvider(gson, preferences, accountRepository)
    }

    @Provides
    @FeatureScope
    fun providePushSubscriptionService(
        prefs: Preferences,
        chainRegistry: ChainRegistry,
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
        accountRepository: AccountRepository
    ): PushSubscriptionService {
        return RealPushSubscriptionService(
            prefs,
            chainRegistry,
            googleApiAvailabilityProvider,
            accountRepository
        )
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsService(
        pushSettingsProvider: PushSettingsProvider,
        pushSubscriptionService: PushSubscriptionService,
        rootScope: RootScope,
        preferences: Preferences,
        pushTokenCache: PushTokenCache,
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
    ): PushNotificationsService {
        return RealPushNotificationsService(
            pushSettingsProvider,
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
        pushNotificationsService: PushNotificationsService,
        pushSettingsProvider: PushSettingsProvider
    ): PushNotificationsInteractor {
        return RealPushNotificationsInteractor(pushNotificationsService, pushSettingsProvider)
    }

    @Provides
    @FeatureScope
    fun provideWelcomePushNotificationsInteractor(preferences: Preferences): WelcomePushNotificationsInteractor {
        return RealWelcomePushNotificationsInteractor(preferences)
    }
}
