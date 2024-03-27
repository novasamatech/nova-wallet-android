package io.novafoundation.nova.feature_cloud_backup_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_impl.BuildConfig
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.GoogleDriveBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.serializer.CloudBackupSerializer
import io.novafoundation.nova.feature_cloud_backup_impl.data.serializer.JsonCloudBackupSerializer
import io.novafoundation.nova.feature_cloud_backup_impl.domain.RealCloudBackupService

@Module
internal class CloudBackupFeatureModule {

    @Provides
    @FeatureScope
    fun provideCloudStorage(
        contextManager: ContextManager,
        systemCallExecutor: SystemCallExecutor,
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
    ): CloudBackupStorage {
        return GoogleDriveBackupStorage(
            contextManager = contextManager,
            systemCallExecutor = systemCallExecutor,
            oauthClientId = BuildConfig.GOOGLE_OAUTH_ID,
            googleApiAvailabilityProvider = googleApiAvailabilityProvider
        )
    }

    @Provides
    @FeatureScope
    fun provideBackupSerializer(
        gson: Gson
    ): CloudBackupSerializer {
        return JsonCloudBackupSerializer(gson)
    }

    @Provides
    @FeatureScope
    fun provideCloudBackupService(
        cloudBackupStorage: CloudBackupStorage,
        backupSerializer: CloudBackupSerializer,
    ): CloudBackupService {
        return RealCloudBackupService(cloudBackupStorage, backupSerializer)
    }
}
