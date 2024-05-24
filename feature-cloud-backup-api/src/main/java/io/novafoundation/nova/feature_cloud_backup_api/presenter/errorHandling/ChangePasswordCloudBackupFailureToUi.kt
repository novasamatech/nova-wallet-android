package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.toCustomDialogPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupInvalidPassword
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupUnknownError

fun mapChangePasswordValidationStatusToUi(
    resourceManager: ResourceManager,
    status: Throwable,
    initSignIn: () -> Unit
): CustomDialogDisplayer.Payload {
    return when (status) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager, initSignIn)

        is CloudBackupNotEnoughSpace -> handleCloudBackupNotEnoughSpace(resourceManager).toCustomDialogPayload(resourceManager)

        is InvalidBackupPasswordError -> handleCloudBackupInvalidPassword(resourceManager).toCustomDialogPayload(resourceManager)

        else -> handleCloudBackupUnknownError(resourceManager).toCustomDialogPayload(resourceManager)
    }
}
