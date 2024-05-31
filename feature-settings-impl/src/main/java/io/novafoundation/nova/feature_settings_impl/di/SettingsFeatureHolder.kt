package io.novafoundation.nova.feature_settings_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

import javax.inject.Inject

@ApplicationScope
class SettingsFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
    private val changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
    private val restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
    private val router: SettingsRouter,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerSettingsFeatureComponent_SettingsFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .versionsFeatureApi(getFeature(VersionsFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .walletConnectFeatureApi(getFeature(WalletConnectFeatureApi::class.java))
            .pushNotificationsFeatureApi(getFeature(PushNotificationsFeatureApi::class.java))
            .cloudBackupFeatureApi(getFeature(CloudBackupFeatureApi::class.java))
            .build()

        return DaggerSettingsFeatureComponent.factory()
            .create(
                router = router,
                syncWalletsBackupPasswordCommunicator = syncWalletsBackupPasswordCommunicator,
                changeBackupPasswordCommunicator = changeBackupPasswordCommunicator,
                restoreBackupPasswordCommunicator = restoreBackupPasswordCommunicator,
                deps = accountFeatureDependencies
            )
    }
}
