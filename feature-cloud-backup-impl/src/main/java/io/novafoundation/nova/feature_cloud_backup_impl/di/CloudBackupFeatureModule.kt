package io.novafoundation.nova.feature_cloud_backup_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_impl.domain.RealCloudBackupService
import io.novafoundation.nova.common.di.scope.FeatureScope

@Module
class CloudBackupFeatureModule {

    @Provides
    @FeatureScope
    fun provideCloudBackupService(): CloudBackupService {
        return RealCloudBackupService()
    }
}
