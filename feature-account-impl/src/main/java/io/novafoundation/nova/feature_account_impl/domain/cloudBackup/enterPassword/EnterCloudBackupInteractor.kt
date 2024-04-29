package io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword

import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.cloudBackup.applyNonDestructiveCloudVersionOrThrow
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.initEnabledBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError

interface EnterCloudBackupInteractor {

    suspend fun restoreCloudBackup(password: String): Result<Unit>

    suspend fun deleteCloudBackup(): Result<Unit>

    suspend fun confirmCloudBackupPassword(password: String): Result<Unit>

    suspend fun restoreCloudBackupPassword(password: String): Result<Unit>
}

class RealEnterCloudBackupInteractor(
    private val cloudBackupService: CloudBackupService,
    private val cloudBackupFacade: LocalAccountsCloudBackupFacade,
    private val accountRepository: AccountRepository
) : EnterCloudBackupInteractor {

    override suspend fun restoreCloudBackup(password: String): Result<Unit> {
        return cloudBackupService.fetchAndDecryptExistingBackup(password)
            .mapCatching { cloudBackup ->
                // `CannotApplyNonDestructiveDiff` shouldn't actually happen here since it is a import for clean app but we should handle it anyway
                cloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(cloudBackup, BackupDiffStrategy.importFromCloud())

                val firstSelectedMetaAccount = accountRepository.getActiveMetaAccounts().first()
                accountRepository.selectMetaAccount(firstSelectedMetaAccount.id)

                cloudBackupService.session.initEnabledBackup(password)
            }
    }

    override suspend fun deleteCloudBackup(): Result<Unit> {
        return cloudBackupService.deleteBackup()
    }

    override suspend fun confirmCloudBackupPassword(password: String): Result<Unit> {
        return cloudBackupService.session.getSavedPassword()
            .mapCatching {
                if (it != password) {
                    throw InvalidBackupPasswordError()
                }
            }
    }

    override suspend fun restoreCloudBackupPassword(password: String): Result<Unit> {
        return cloudBackupService.fetchAndDecryptExistingBackup(password)
            .map { cloudBackupService.session.setSavedPassword(password) }
    }
}
