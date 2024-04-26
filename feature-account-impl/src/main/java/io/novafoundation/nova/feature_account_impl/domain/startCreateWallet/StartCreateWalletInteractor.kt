package io.novafoundation.nova.feature_account_impl.domain.startCreateWallet

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus

interface StartCreateWalletInteractor {

    suspend fun validateCanCreateBackup(): PreCreateValidationStatus

    suspend fun signInToCloud(): Result<Unit>
}

class RealStartCreateWalletInteractor(
    private val cloudBackupService: CloudBackupService
) : StartCreateWalletInteractor {

    override suspend fun validateCanCreateBackup(): PreCreateValidationStatus {
        return cloudBackupService.validateCanCreateBackup()
    }

    override suspend fun signInToCloud(): Result<Unit> {
        return cloudBackupService.signInToCloud()
    }
}
