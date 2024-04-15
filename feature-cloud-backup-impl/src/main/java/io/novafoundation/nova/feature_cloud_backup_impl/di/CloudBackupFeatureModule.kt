package io.novafoundation.nova.feature_cloud_backup_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_impl.BuildConfig
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.CloudBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage.GoogleDriveBackupStorage
import io.novafoundation.nova.feature_cloud_backup_impl.data.encryption.CloudBackupEncryption
import io.novafoundation.nova.feature_cloud_backup_impl.data.encryption.ScryptCloudBackupEncryption
import io.novafoundation.nova.feature_cloud_backup_impl.data.preferences.CloudBackupPreferences
import io.novafoundation.nova.feature_cloud_backup_impl.data.preferences.SharedPrefsCloudBackupPreferences
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
    fun provideBackupSerializer(): CloudBackupSerializer {
        return JsonCloudBackupSerializer()
    }

    @Provides
    @FeatureScope
    fun provideBackupEncryption(): CloudBackupEncryption {
        return ScryptCloudBackupEncryption()
    }

    @Provides
    @FeatureScope
    fun provideBackupPreferences(preferences: Preferences, encryptedPreferences: EncryptedPreferences): CloudBackupPreferences {
        return SharedPrefsCloudBackupPreferences(preferences, encryptedPreferences)
    }

    @Provides
    @FeatureScope
    fun provideCloudBackupService(
        cloudBackupStorage: CloudBackupStorage,
        backupSerializer: CloudBackupSerializer,
        encryption: CloudBackupEncryption,
        backupPreferences: CloudBackupPreferences,
    ): CloudBackupService {
        return RealCloudBackupService(
            storage = cloudBackupStorage,
            serializer = backupSerializer,
            encryption = encryption,
            cloudBackupPreferences = backupPreferences
        )
    }
}
