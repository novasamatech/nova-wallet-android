package io.novafoundation.nova.feature_account_impl.domain.startCreateWallet

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus

interface StartCreateWalletInteractor {

    suspend fun validateCanCreateBackup(): PreCreateValidationStatus
}

class RealStartCreateWalletInteractor(
    private val cloudBackupService: CloudBackupService
) : StartCreateWalletInteractor {

    override suspend fun validateCanCreateBackup(): PreCreateValidationStatus {
        return cloudBackupService.validateCanCreateBackup()
    }
}
