package io.novafoundation.nova.feature_settings_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.settings.di.SettingsComponent
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi

@Component(
    dependencies = [
        SettingsFeatureDependencies::class,
    ],
    modules = [
        SettingsFeatureModule::class,
    ]
)
@FeatureScope
interface SettingsFeatureComponent : SettingsFeatureApi {

    fun settingsComponentFactory(): SettingsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: SettingsRouter,
            deps: SettingsFeatureDependencies
        ): SettingsFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            CurrencyFeatureApi::class,
            AccountFeatureApi::class,
            WalletConnectFeatureApi::class,
            VersionsFeatureApi::class,
            PushNotificationsFeatureApi::class
        ]
    )
    interface SettingsFeatureDependenciesComponent : SettingsFeatureDependencies
}
