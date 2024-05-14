package io.novafoundation.nova.feature_account_impl.domain.startCreateWallet

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus

interface StartCreateWalletInteractor {

    suspend fun validateCanCreateBackup(): PreCreateValidationStatus

    suspend fun signInToCloud(): Result<Unit>

    suspend fun isSyncWithCloudEnabled(): Boolean

    suspend fun createWallet(name: String): Result<Unit>
}

class RealStartCreateWalletInteractor(
    private val cloudBackupService: CloudBackupService,
    private val addAccountInteractor: AddAccountInteractor,
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

    override suspend fun createWallet(name: String): Result<Unit> {
        return addAccountInteractor.createMetaAccountWithRecommendedSettings(AddAccountType.MetaAccount(name))
    }
}
