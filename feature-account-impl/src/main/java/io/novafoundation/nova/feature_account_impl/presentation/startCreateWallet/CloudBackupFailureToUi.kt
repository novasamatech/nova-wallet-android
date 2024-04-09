package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupConnectionError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupServiceUnavailable
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupUnknownError

fun mapPreCreateValidationStatusToUi(
    resourceManager: ResourceManager,
    status: PreCreateValidationStatus,
    existingBackupFound: () -> Unit
): TitleAndMessage? {
    return when (status) {
        is PreCreateValidationStatus.AuthenticationFailed -> handleCloudBackupAuthFailed(resourceManager)

        is PreCreateValidationStatus.BackupServiceUnavailable -> handleCloudBackupServiceUnavailable(resourceManager)

        is PreCreateValidationStatus.ExistingBackupFound -> {
            existingBackupFound()
            null
        }

        is PreCreateValidationStatus.NotEnoughSpace -> handleCloudBackupNotEnoughSpace(resourceManager)

        is PreCreateValidationStatus.ConnectionError -> handleCloudBackupConnectionError(resourceManager)

        is PreCreateValidationStatus.OtherError -> handleCloudBackupUnknownError(resourceManager)

        is PreCreateValidationStatus.Ok -> null
    }
}
