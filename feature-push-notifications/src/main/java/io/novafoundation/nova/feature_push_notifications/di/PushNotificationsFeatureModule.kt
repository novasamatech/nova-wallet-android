package io.novafoundation.nova.feature_push_notifications.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_push_notifications.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.PushPermissionRepository
import io.novafoundation.nova.feature_push_notifications.data.PushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.RealPushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.RealPushPermissionRepository
import io.novafoundation.nova.feature_push_notifications.data.RealPushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsSerializer
import io.novafoundation.nova.feature_push_notifications.data.settings.RealPushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.subscription.PushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.subscription.RealPushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.domain.interactor.GovernancePushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealGovernancePushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealPushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealStakingPushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealWelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.StakingPushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
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
    fun providePushPermissionRepository(context: Context): PushPermissionRepository {
        return RealPushPermissionRepository(context)
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsService(
        pushSettingsProvider: PushSettingsProvider,
        pushSubscriptionService: PushSubscriptionService,
        rootScope: RootScope,
        pushTokenCache: PushTokenCache,
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
        pushPermissionRepository: PushPermissionRepository,
        preferences: Preferences
    ): PushNotificationsService {
        return RealPushNotificationsService(
            pushSettingsProvider,
            pushSubscriptionService,
            rootScope,
            pushTokenCache,
            googleApiAvailabilityProvider,
            pushPermissionRepository,
            preferences
        )
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsInteractor(
        pushNotificationsService: PushNotificationsService,
        pushSettingsProvider: PushSettingsProvider,
        accountRepository: AccountRepository
    ): PushNotificationsInteractor {
        return RealPushNotificationsInteractor(pushNotificationsService, pushSettingsProvider, accountRepository)
    }

    @Provides
    @FeatureScope
    fun provideWelcomePushNotificationsInteractor(preferences: Preferences): WelcomePushNotificationsInteractor {
        return RealWelcomePushNotificationsInteractor(preferences)
    }

    @Provides
    @FeatureScope
    fun provideGovernancePushSettingsInteractor(
        chainRegistry: ChainRegistry,
        governanceSourceRegistry: GovernanceSourceRegistry
    ): GovernancePushSettingsInteractor {
        return RealGovernancePushSettingsInteractor(
            chainRegistry,
            governanceSourceRegistry
        )
    }

    @Provides
    @FeatureScope
    fun provideStakingPushSettingsInteractor(chainRegistry: ChainRegistry): StakingPushSettingsInteractor {
        return RealStakingPushSettingsInteractor(chainRegistry)
    }
}
