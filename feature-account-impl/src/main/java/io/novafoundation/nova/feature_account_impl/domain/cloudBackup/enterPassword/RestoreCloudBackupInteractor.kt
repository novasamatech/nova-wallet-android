package io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.applyNonDestructiveCloudVersionOrThrow
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackup

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
                cloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(cloudBackup)

                val firstSelectedMetaAccount = accountRepository.getActiveMetaAccounts().first()
                accountRepository.selectMetaAccount(firstSelectedMetaAccount.id)
            }
    }

    override suspend fun deleteCloudBackup(): Result<Unit> {
        return cloudBackupService.deleteBackup()
    }
}
