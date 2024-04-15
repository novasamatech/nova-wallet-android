package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.utils.finally
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.cloudBackup.applyNonDestructiveCloudVersionOrThrow
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackupWithSavedPassword
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy
import io.novafoundation.nova.feature_cloud_backup_api.domain.setLastSyncedTimeAsNow
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface CloudBackupSettingsInteractor {

    suspend fun isSyncCloudBackupEnabled(): Boolean

    fun observeLastSyncedTime(): Flow<Date?>

    suspend fun syncCloudBackup(): Result<Unit>

    suspend fun setCloudBackupSyncEnabled(enable: Boolean)
}

class RealCloudBackupSettingsInteractor(
    private val cloudBackupService: CloudBackupService,
    private val cloudBackupFacade: LocalAccountsCloudBackupFacade
) : CloudBackupSettingsInteractor {

    override fun observeLastSyncedTime(): Flow<Date?> {
        return cloudBackupService.session.lastSyncedTimeFlow()
    }

    override suspend fun syncCloudBackup(): Result<Unit> {
        return cloudBackupService.fetchAndDecryptExistingBackupWithSavedPassword()
            .mapCatching { cloudBackup ->
                cloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(cloudBackup, BackupDiffStrategy.syncWithCloud())

                Unit
            }.finally {
                cloudBackupService.session.setLastSyncedTimeAsNow()
            }
    }

    override suspend fun setCloudBackupSyncEnabled(enable: Boolean) {
        cloudBackupService.session.setSyncingBackupEnabled(enable)
    }

    override suspend fun isSyncCloudBackupEnabled(): Boolean {
        return cloudBackupService.session.isSyncWithCloudEnabled()
    }
}
