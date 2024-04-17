package io.novafoundation.nova.feature_settings_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.di.CloudBackupSettingsComponent
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

    fun backupSettings(): CloudBackupSettingsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: SettingsRouter,
            @BindsInstance syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
            @BindsInstance changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
            @BindsInstance restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
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
            PushNotificationsFeatureApi::class,
            CloudBackupFeatureApi::class
        ]
    )
    interface SettingsFeatureDependenciesComponent : SettingsFeatureDependencies
}
