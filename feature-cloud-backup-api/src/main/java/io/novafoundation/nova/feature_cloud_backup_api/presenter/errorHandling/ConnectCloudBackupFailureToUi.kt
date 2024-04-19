package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.toCustomDialogPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.PreCreateValidationStatus
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupServiceUnavailable
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupUnknownError

fun mapPreCreateValidationStatusToUi(
    resourceManager: ResourceManager,
    status: PreCreateValidationStatus,
    existingBackupFound: () -> Unit,
    initSignIn: () -> Unit
): CustomDialogDisplayer.Payload? {
    return when (status) {
        is PreCreateValidationStatus.AuthenticationFailed -> handleCloudBackupAuthFailed(resourceManager, initSignIn)

        is PreCreateValidationStatus.BackupServiceUnavailable -> handleCloudBackupServiceUnavailable(resourceManager).toCustomDialogPayload(resourceManager)

        is PreCreateValidationStatus.ExistingBackupFound -> {
            existingBackupFound()
            null
        }

        is PreCreateValidationStatus.NotEnoughSpace -> handleCloudBackupNotEnoughSpace(resourceManager).toCustomDialogPayload(resourceManager)

        is PreCreateValidationStatus.OtherError -> handleCloudBackupUnknownError(resourceManager).toCustomDialogPayload(resourceManager)

        is PreCreateValidationStatus.Ok -> null
    }
}
