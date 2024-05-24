package io.novafoundation.nova.feature_settings_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.domain.RealCloudBackupSettingsInteractor

@Module
class SettingsFeatureModule {

    @Provides
    @FeatureScope
    fun provideCloudBackupSettingsInteractor(
        cloudBackupService: CloudBackupService,
        cloudBackupFacade: LocalAccountsCloudBackupFacade
    ): CloudBackupSettingsInteractor {
        return RealCloudBackupSettingsInteractor(
            cloudBackupService,
            cloudBackupFacade
        )
    }
}
