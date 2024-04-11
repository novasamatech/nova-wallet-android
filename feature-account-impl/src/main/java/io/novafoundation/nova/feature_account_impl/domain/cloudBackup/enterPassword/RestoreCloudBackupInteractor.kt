package io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.CloudBackupAddMetaAccountRepository
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.fetchAndDecryptExistingBackup

interface RestoreCloudBackupInteractor {

    suspend fun restoreCloudBackup(password: String): Result<Unit>

    suspend fun deleteCloudBackup(): Result<Unit>
}

class RealRestoreCloudBackupInteractor(
    private val cloudBackupService: CloudBackupService,
    private val accountRepository: AccountRepository,
    private val addMetaAccountRepository: CloudBackupAddMetaAccountRepository
) : RestoreCloudBackupInteractor {

    override suspend fun restoreCloudBackup(password: String): Result<Unit> {
        return cloudBackupService.fetchAndDecryptExistingBackup(password)
            .map { cloudBackup ->
                val payloads = cloudBackup.publicData.wallets.map {
                    CloudBackupAddMetaAccountRepository.Payload(it)
                }
                val addAccountResult = addMetaAccountRepository.addAccounts(payloads)
                accountRepository.selectMetaAccount(addAccountResult.first().metaId)
            }
    }

    override suspend fun deleteCloudBackup(): Result<Unit> {
        return cloudBackupService.deleteBackup()
    }
}
