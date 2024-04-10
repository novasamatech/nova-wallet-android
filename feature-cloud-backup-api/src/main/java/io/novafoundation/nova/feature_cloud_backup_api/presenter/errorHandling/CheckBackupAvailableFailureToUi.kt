package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupServiceUnavailable
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError

fun mapCheckBackupAvailableFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable
): TitleAndMessage? {
    return when (throwable) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager)

        is CloudBackupServiceUnavailable -> handleCloudBackupServiceUnavailable(resourceManager)

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        else -> null
    }
}
