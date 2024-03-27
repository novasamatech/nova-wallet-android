package io.novafoundation.nova.feature_cloud_backup_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_cloud_backup_impl.BuildConfig
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.GoogleDriveBackupStorage

@Module
internal class GoogleDriveBackupModule {

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
}
