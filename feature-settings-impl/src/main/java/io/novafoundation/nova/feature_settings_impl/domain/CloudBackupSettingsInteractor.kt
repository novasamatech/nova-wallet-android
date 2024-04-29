package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.finally
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.cloudBackup.applyNonDestructiveCloudVersionOrThrow
import io.novafoundation.nova.feature_account_api.data.cloudBackup.toMetaAccountType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.metaAccountTypeComparator
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackupWithSavedPassword
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy
import io.novafoundation.nova.feature_cloud_backup_api.domain.setLastSyncedTimeAsNow
import io.novafoundation.nova.feature_settings_impl.domain.model.CloudBackupChangedAccount
import io.novafoundation.nova.feature_settings_impl.domain.model.CloudBackupChangedAccount.ChangingType
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface CloudBackupSettingsInteractor {

    suspend fun isSyncCloudBackupEnabled(): Boolean

    fun observeLastSyncedTime(): Flow<Date?>

    suspend fun syncCloudBackup(): Result<Unit>

    suspend fun setCloudBackupSyncEnabled(enable: Boolean)

    suspend fun deleteCloudBackup(): Result<Unit>

    suspend fun writeLocalBackupToCloud(): Result<Unit>

    suspend fun signInToCloud(): Result<Unit>

    fun prepareSortedLocalChangesFromDiff(cloudBackupDiff: CloudBackupDiff): GroupedList<LightMetaAccount.Type, CloudBackupChangedAccount>

    suspend fun applyBackupAccountDiff(cloudBackupDiff: CloudBackupDiff, cloudBackup: CloudBackup): Result<Unit>
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
            }.flatMap {
                writeLocalBackupToCloud()
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

    override suspend fun deleteCloudBackup(): Result<Unit> {
        return cloudBackupService.deleteBackup()
    }

    override suspend fun writeLocalBackupToCloud(): Result<Unit> {
        return cloudBackupService.session.getSavedPassword()
            .flatMap { password ->
                val localSnapshot = cloudBackupFacade.fullBackupInfoFromLocalSnapshot()
                cloudBackupService.writeBackupToCloud(WriteBackupRequest(localSnapshot, password))
            }
    }

    override suspend fun signInToCloud(): Result<Unit> {
        return cloudBackupService.signInToCloud()
    }

    override fun prepareSortedLocalChangesFromDiff(cloudBackupDiff: CloudBackupDiff): GroupedList<LightMetaAccount.Type, CloudBackupChangedAccount> {
        val accounts = localAccountChangesFromDiff(cloudBackupDiff.localChanges)
            .sortedBy { it.account.name }
        return accounts.groupBy { it.account.type.toMetaAccountType() }
            .toSortedMap(metaAccountTypeComparator())
    }

    override suspend fun applyBackupAccountDiff(cloudBackupDiff: CloudBackupDiff, cloudBackup: CloudBackup): Result<Unit> {
        return runCatching {
            cloudBackupFacade.applyBackupDiff(cloudBackupDiff, cloudBackup)
            cloudBackupService.session.setLastSyncedTimeAsNow()
        }.flatMap {
            writeLocalBackupToCloud()
        }
    }

    private fun localAccountChangesFromDiff(diff: CloudBackupDiff.PerSourceDiff): List<CloudBackupChangedAccount> {
        return diff.added.map { CloudBackupChangedAccount(ChangingType.ADDED, it) } +
            diff.modified.map { CloudBackupChangedAccount(ChangingType.CHANGED, it) } +
            diff.removed.map { CloudBackupChangedAccount(ChangingType.REMOVED, it) }
    }
}
