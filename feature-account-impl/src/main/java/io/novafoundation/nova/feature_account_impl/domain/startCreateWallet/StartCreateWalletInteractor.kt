package io.novafoundation.nova.feature_account_impl.domain.startCreateWallet

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StartCreateWalletInteractor {

    suspend fun validateCanCreateBackup(): PreCreateValidationStatus

    suspend fun signInToCloud(): Result<Unit>

    suspend fun isSyncWithCloudEnabled(): Boolean

    suspend fun createWalletAndSelect(name: String): Result<Unit>
}

class RealStartCreateWalletInteractor(
    private val cloudBackupService: CloudBackupService,
    private val addAccountInteractor: AddAccountInteractor,
    private val accountRepository: AccountRepository
) : StartCreateWalletInteractor {

    override suspend fun validateCanCreateBackup(): PreCreateValidationStatus {
        return cloudBackupService.validateCanCreateBackup()
    }

    override suspend fun signInToCloud(): Result<Unit> {
        return cloudBackupService.signInToCloud()
    }

    override suspend fun isSyncWithCloudEnabled(): Boolean {
        return cloudBackupService.session.isSyncWithCloudEnabled()
    }

    override suspend fun createWalletAndSelect(name: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            addAccountInteractor.createMetaAccountWithRecommendedSettings(AddAccountType.MetaAccount(name))
        }
    }
}
