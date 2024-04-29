package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupUnknownError

fun mapWriteBackupFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable
): TitleAndMessage {
    return when (throwable) {
        is CloudBackupNotEnoughSpace -> handleCloudBackupNotEnoughSpace(resourceManager)

        else -> handleCloudBackupUnknownError(resourceManager)
    }
}
