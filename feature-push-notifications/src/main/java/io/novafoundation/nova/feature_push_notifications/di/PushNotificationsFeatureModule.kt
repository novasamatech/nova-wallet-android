package io.novafoundation.nova.feature_push_notifications.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.BuildTypeProvider
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.PushPermissionRepository
import io.novafoundation.nova.feature_push_notifications.data.PushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.RealPushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.RealPushPermissionRepository
import io.novafoundation.nova.feature_push_notifications.data.RealPushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.repository.MultisigPushAlertRepository
import io.novafoundation.nova.feature_push_notifications.data.repository.PushSettingsRepository
import io.novafoundation.nova.feature_push_notifications.data.repository.RealMultisigPushAlertRepository
import io.novafoundation.nova.feature_push_notifications.data.repository.RealPushSettingsRepository
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsSerializer
import io.novafoundation.nova.feature_push_notifications.data.settings.RealPushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.subscription.PushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.subscription.RealPushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.domain.interactor.GovernancePushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.MultisigPushAlertInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealGovernancePushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealMultisigPushAlertInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealPushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealStakingPushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.RealWelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.StakingPushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.presentation.multisigsWarning.MultisigPushNotificationsAlertMixinFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PushSettingsSerialization

@Module(includes = [NotificationHandlersModule::class])
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
        preferences: Preferences,
        buildTypeProvider: BuildTypeProvider
    ): PushNotificationsService {
        return RealPushNotificationsService(
            pushSettingsProvider,
            pushSubscriptionService,
            rootScope,
            pushTokenCache,
            googleApiAvailabilityProvider,
            pushPermissionRepository,
            preferences,
            buildTypeProvider
        )
    }

    @Provides
    @FeatureScope
    fun providePushSettingsRepository(preferences: Preferences): PushSettingsRepository {
        return RealPushSettingsRepository(preferences)
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsInteractor(
        pushNotificationsService: PushNotificationsService,
        pushSettingsProvider: PushSettingsProvider,
        accountRepository: AccountRepository,
        pushSettingsRepository: PushSettingsRepository
    ): PushNotificationsInteractor {
        return RealPushNotificationsInteractor(pushNotificationsService, pushSettingsProvider, accountRepository, pushSettingsRepository)
    }

    @Provides
    @FeatureScope
    fun provideWelcomePushNotificationsInteractor(
        preferences: Preferences,
        pushNotificationsService: PushNotificationsService
    ): WelcomePushNotificationsInteractor {
        return RealWelcomePushNotificationsInteractor(preferences, pushNotificationsService)
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

    @Provides
    @FeatureScope
    fun provideMultisigPushAlertRepository(
        preferences: Preferences
    ): MultisigPushAlertRepository {
        return RealMultisigPushAlertRepository(preferences)
    }

    @Provides
    @FeatureScope
    fun provideMultisigPushAlertInteractor(
        pushSettingsProvider: PushSettingsProvider,
        accountRepository: AccountRepository,
        multisigPushAlertRepository: MultisigPushAlertRepository
    ): MultisigPushAlertInteractor {
        return RealMultisigPushAlertInteractor(
            pushSettingsProvider,
            accountRepository,
            multisigPushAlertRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideMultisigPushNotificationsAlertMixin(
        automaticInteractionGate: AutomaticInteractionGate,
        interactor: MultisigPushAlertInteractor,
        metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
        router: PushNotificationsRouter
    ): MultisigPushNotificationsAlertMixinFactory {
        return MultisigPushNotificationsAlertMixinFactory(
            automaticInteractionGate,
            interactor,
            metaAccountsUpdatesRegistry,
            router
        )
    }
}
