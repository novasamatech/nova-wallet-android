package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError

fun mapChangePasswordValidationStatusToUi(
    resourceManager: ResourceManager,
    status: Throwable
): TitleAndMessage {
    return when (status) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager)

        is CloudBackupNotEnoughSpace -> handleCloudBackupNotEnoughSpace(resourceManager)

        is InvalidBackupPasswordError -> handleCloudBackupInvalidPassword(resourceManager)

        else -> handleCloudBackupUnknownError(resourceManager)
    }
}
