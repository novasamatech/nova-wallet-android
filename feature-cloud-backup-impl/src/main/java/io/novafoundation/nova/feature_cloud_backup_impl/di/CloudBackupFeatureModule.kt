package io.novafoundation.nova.feature_cloud_backup_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.di.modules.GoogleDriveBackupModule
import io.novafoundation.nova.feature_cloud_backup_impl.domain.RealCloudBackupService

@Module(includes = [GoogleDriveBackupModule::class])
internal class CloudBackupFeatureModule {

    @Provides
    @FeatureScope
    fun provideCloudBackupService(cloudBackupStorage: CloudBackupStorage): CloudBackupService {
        return RealCloudBackupService(cloudBackupStorage)
    }
}
