package io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword

import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.applyNonDestructiveCloudVersionOrThrow
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.isEmpty
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy

interface RestoreCloudBackupInteractor {

    suspend fun restoreCloudBackup(password: String): Result<Unit>

    suspend fun deleteCloudBackup(): Result<Unit>
}

class RealRestoreCloudBackupInteractor(
    private val cloudBackupService: CloudBackupService,
    private val cloudBackupFacade: LocalAccountsCloudBackupFacade,
    private val accountRepository: AccountRepository,
) : RestoreCloudBackupInteractor {

    override suspend fun restoreCloudBackup(password: String): Result<Unit> {
        return cloudBackupService.fetchAndDecryptExistingBackup(password)
            .mapCatching { cloudBackup ->
                // `CannotApplyNonDestructiveDiff` shouldn't actually happen here since it is a import for clean app but we should handle it anyway
                val diff = cloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(cloudBackup, BackupDiffStrategy.importFromCloud())

                val firstSelectedMetaAccount = accountRepository.getActiveMetaAccounts().first()
                accountRepository.selectMetaAccount(firstSelectedMetaAccount.id)

                diff
            }.flatMap { diff ->
                // Once we successfully applied state locally, we can write new changes to backup to sync backup with other local state
                if (diff.cloudChanges.isEmpty()) return Result.success(Unit)

                val request = WriteBackupRequest(
                    cloudBackup = cloudBackupFacade.fullBackupInfoFromLocalSnapshot(),
                    password = password
                )
                cloudBackupService.writeBackupToCloud(request)
            }
    }

    override suspend fun deleteCloudBackup(): Result<Unit> {
        return cloudBackupService.deleteBackup()
    }
}
