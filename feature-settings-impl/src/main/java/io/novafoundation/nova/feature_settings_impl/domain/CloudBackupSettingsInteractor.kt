package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.cloudBackup.applyNonDestructiveCloudVersionOrThrow
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy
import java.util.*
import kotlinx.coroutines.flow.Flow

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
        return cloudBackupService.observeLastSyncedTime()
    }

    override suspend fun syncCloudBackup(): Result<Unit> {
        return cloudBackupService.fetchAndDecryptExistingBackup("q1234567") // TODO: remove hardcoded password
            .mapCatching { cloudBackup ->
                cloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(cloudBackup, BackupDiffStrategy.importFromCloud())
                cloudBackupService.setLastSyncedTime(Date())
            }
    }

    override suspend fun setCloudBackupSyncEnabled(enable: Boolean) {
        cloudBackupService.setSyncingBackupEnabled(enable)
    }

    override suspend fun isSyncCloudBackupEnabled(): Boolean {
        return cloudBackupService.isSyncWithCloudEnabled()
    }
}
